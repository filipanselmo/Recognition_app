import base64
import logging
import os
import uuid
from PIL import Image, ImageDraw

import torch
from torchvision import transforms
from flask import Flask, render_template, send_from_directory, request, jsonify
from flask_cors import CORS
from scipy import io

from OptimizedYOLO import OptimizedYOLO
from dbModels import db, DetectionResult, Photo, Planogram, ComplianceCheckResult, Embedding
from gan import augment_image
from triplet_net import TripletNet

app = Flask(__name__)
app.config.from_object('config.Config')
CORS(app)
db.init_app(app)

yolo = OptimizedYOLO()
triplet_net = TripletNet()


# Расчет Intersection over Union
def calculate_iou(box1, box2):
    xA = max(box1[0], box2[0])
    yA = max(box1[1], box2[1])
    xB = min(box1[2], box2[2])
    yB = min(box1[3], box2[3])

    inter_area = max(0, xB - xA) * max(0, yB - yA)
    box1_area = (box1[2] - box1[0]) * (box1[3] - box1[1])
    box2_area = (box2[2] - box2[0]) * (box2[3] - box2[1])
    iou = inter_area / float(box1_area + box2_area - inter_area)
    return iou


def generate_recommendations(compliance_checks):
    recommendations = []
    for check in compliance_checks:
        if check.missing_count > 0:
            recommendations.append({
                'sku': check.sku,
                'action': 'restock',
                'quantity': check.missing_count,
                'priority': 'high' if check.missing_count > 2 else 'medium'
            })
        if check.extra_count > 0:
            recommendations.append({
                'sku': check.sku,
                'action': 'remove',
                'quantity': check.extra_count,
                'priority': 'low'
            })
    return recommendations


def draw_boxes(photo):
    try:
        image = Image.open(io.BytesIO(photo.content))
        draw = ImageDraw.Draw(image)
        width, height = image.size

        # Отрисовка планограммы
        planogram_entries = Planogram.query.filter_by(shelf_id=photo.shelf_id).all()
        for entry in planogram_entries:
            x_min = entry.x_min * width
            y_min = entry.y_min * height
            x_max = entry.x_max * width
            y_max = entry.y_max * height
            draw.rectangle([x_min, y_min, x_max, y_max], outline="blue", width=3)
            draw.text((x_min, y_min - 15), f"Plan: {entry.sku}", fill="blue")

        # Отрисовка детекций
        detections = DetectionResult.query.filter_by(photo_id=photo.id).all()
        for det in detections:
            x_min = det.x_min * width
            y_min = det.y_min * height
            x_max = det.x_max * width
            y_max = det.y_max * height
            draw.rectangle([x_min, y_min, x_max, y_max], outline="green", width=2)
            draw.text((x_min, y_max + 5), f"{det.label} {det.confidence:.2f}", fill="green")

        # Отрисовка отклонений
        compliance_checks = ComplianceCheckResult.query.filter_by(photo_id=photo.id).all()
        for check in compliance_checks:
            if check.missing_count > 0:
                entry = Planogram.query.filter_by(shelf_id=photo.shelf_id, sku=check.sku).first()
                if entry:
                    x_min = entry.x_min * width
                    y_min = entry.y_min * height
                    x_max = entry.x_max * width
                    y_max = entry.y_max * height
                    draw.rectangle([x_min, y_min, x_max, y_max], outline="red", width=3)
                    draw.text((x_min, y_min - 30), f"Missing: {check.missing_count}", fill="red")

        buf = io.BytesIO()
        image.save(buf, format='PNG')
        return base64.b64encode(buf.getvalue()).decode('utf-8')
    except Exception as e:
        print(f"Error drawing boxes: {str(e)}")
        return None


@app.route('/', methods=['GET'])
def home():
    page = request.args.get('page', 1, type=int)
    per_page = 10
    paged_results = DetectionResult.query.order_by(DetectionResult.photo_id).paginate(page=page, per_page=per_page,
                                                                                      error_out=False)
    photos = Photo.query.all()
    return render_template('index.html', photos=photos, paged_results=paged_results)


# Добавляем новый маршрут для загрузки файлов из папки 'uploads'
@app.route('/uploads/<filename>')
def uploaded_file(filename):
    return send_from_directory(os.path.join(app.root_path, 'uploads'), filename)


@app.route('/upload', methods=['POST'])
def upload_photo():
    if 'file' not in request.files:
        return jsonify({'error': 'No file part'}), 400

    file = request.files['file']
    shelf_id = request.form.get('shelf_id')
    if not shelf_id:
        return jsonify({'error': 'shelf_id is required'}), 400

    # Сохранение файла
    filename = f"{uuid.uuid4()}_{file.filename}"
    image = Image.open(file.stream)
    augmented_image = augment_image(image)
    img_byte_arr = io.BytesIO()
    augmented_image.save(img_byte_arr, format='PNG')
    photo = Photo(
        filename=filename,
        content=img_byte_arr.getvalue(),
        shelf_id=shelf_id
    )
    db.session.add(photo)
    db.session.commit()

    # Детектирование
    detections = yolo.detect(augmented_image)
    for det in detections:
        detection = DetectionResult(
            photo_id=photo.id,
            label=det['class'],
            confidence=det['confidence'],
            x_min=det['bbox'][0],
            y_min=det['bbox'][1],
            x_max=det['bbox'][2],
            y_max=det['bbox'][3]
        )
        db.session.add(detection)
    db.session.commit()

    # Генерация эмбеддингов
    img_tensor = transforms.ToTensor()(augmented_image).unsqueeze(0)
    with torch.no_grad():
        embedding = triplet_net(img_tensor).numpy()
    emb = Embedding(photo_id=photo.id, features=embedding.tobytes())
    db.session.add(emb)
    db.session.commit()

    # Проверка соответствия планограмме
    check_compliance(photo.id, shelf_id)
    return jsonify({'message': 'Success'}), 201


def check_compliance(photo_id, shelf_id):
    # Получение планограммы для полки
    planogram_entries = Planogram.query.filter_by(shelf_id=shelf_id).all()
    detected_boxes = DetectionResult.query.filter_by(photo_id=photo_id).all()

    sku_stats = {}
    for entry in planogram_entries:
        sku = entry.sku
        expected_quantity = entry.quantity
        sku_stats[sku] = {
            'expected': expected_quantity,
            'detected': 0,
            'missing': 0,
            'extra': 0
        }

    # Подсчет обнаруженных товаров
    for detection in detected_boxes:
        matched = False
        for entry in planogram_entries:
            if detection.label == entry.sku:
                iou = calculate_iou(
                    [detection.x_min, detection.y_min, detection.x_max, detection.y_max],
                    [entry.x_min, entry.y_min, entry.x_max, entry.y_max]
                )
                if iou > 0.5:  # Порог IoU для совпадения
                    sku_stats[entry.sku]['detected'] += 1
                    matched = True
                    break
        if not matched:
            # Лишний товар
            compliance_result = ComplianceCheckResult(
                photo_id=photo_id,
                sku=detection.label,
                missing_count=0,
                extra_count=1
            )
            db.session.add(compliance_result)

    # Вычисление недостачи
    for sku, stats in sku_stats.items():
        missing = stats['expected'] - stats['detected']
        if missing > 0:
            compliance_result = ComplianceCheckResult(
                photo_id=photo_id,
                sku=sku,
                missing_count=missing,
                extra_count=0
            )
            db.session.add(compliance_result)

    db.session.commit()


@app.route('/photos', methods=['GET'])
def get_photos():
    photos = Photo.query.all()
    results = []

    for photo in photos:
        annotated_image = draw_boxes(photo)

        compliance_checks = ComplianceCheckResult.query.filter_by(photo_id=photo.id).all()
        compliance_info = [{
            'sku': check.sku,
            'missing': check.missing_count,
            'extra': check.extra_count
        } for check in compliance_checks]

        recommendations = generate_recommendations(compliance_checks)

        photo_data = {
            'id': photo.id,
            'filename': photo.filename,
            'shelf_id': photo.shelf_id,
            'annotated_image': annotated_image,
            'compliance_info': compliance_info,
            'recommendations': recommendations
        }
        results.append(photo_data)

    return jsonify(results), 200


@app.route('/detection-results', methods=['GET'])
def get_detection_results():
    results = DetectionResult.query.all()
    return jsonify([
        {
            'id': result.id,
            'photo_id': result.photo_id,
            'label': result.label,
            'confidence': result.confidence,
            'x_min': result.x_min,
            'y_min': result.y_min,
            'x_max': result.x_max,
            'y_max': result.y_max,
            'created_at': result.created_at.isoformat(),
        }
        for result in results
    ]), 200


@app.route('/photos-with-results', methods=['GET'])
def get_photos_with_results():
    photos = Photo.query.all()
    results = DetectionResult.query.all()

    # Группируем результаты обнаружения по фотографиям
    grouped_results = {}
    for result in results:
        if result.photo_id not in grouped_results:
            grouped_results[result.photo_id] = []
        grouped_results[result.photo_id].append({
            'id': result.id,
            'label': result.label,
            'confidence': result.confidence,
            'x_min': result.x_min,
            'y_min': result.y_min,
            'x_max': result.x_max,
            'y_max': result.y_max,
            'created_at': result.created_at.isoformat(),
        })

    # Формируем ответ
    response = []
    for photo in photos:
        content = base64.b64encode(photo.content).decode('ascii')
        # Проверяем наличие фото перед добавлением в ответ
        if photo is not None:
            response.append({
                'id': photo.id,
                'filename': photo.filename,
                'content': content,
                'results': grouped_results.get(photo.id, [])
            })
    return jsonify(response), 200


@app.errorhandler(500)
def internal_error(error):
    logging.exception("An error occurred: %s", error)
    return "Internal Server Error", 500


if __name__ == '__main__':
    with app.app_context():
        db.create_all()
        app.run(host='0.0.0.0', port=5000, debug=True)
