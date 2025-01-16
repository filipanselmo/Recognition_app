from flask import Flask, render_template, send_from_directory, request, jsonify
from flask_cors import CORS
from dbModels import db, DetectionResult, Photo
from yolo import YOLO
import uuid
import os
import json
import logging
import base64

app = Flask(__name__)
app.config.from_object('config.Config')
CORS(app)
db.init_app(app)

yolo = YOLO()


@app.route('/', methods=['GET'])
def home():
    page = request.args.get('page', 1, type=int)
    per_page = 10
    paged_results = DetectionResult.query.order_by(DetectionResult.photo_id).paginate(page=page, per_page=per_page, error_out=False)
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
    if file.filename == '':
        return jsonify({'error': 'No selected file'}), 400

    # Сохранение файла временно на диск

    # Генерация нового имени файла
    new_filename = f"{uuid.uuid4().hex}_{file.filename}"
    # # Путь к новому файлу
    file_path = os.path.join('uploads', new_filename)
    # Сохранение файла под новым именем
    file.save(file_path)


    # Обработка изображения с помощью YOLO
    results_json = yolo.detect_objects(file_path)

    # Преобразуем строку JSON в список словарей
    try:
        results_json = json.loads(results_json)
        print("Detection Results:", results_json)  # Добавьте эту строку для отладки
    except json.JSONDecodeError as e:
        return jsonify({'error': 'Failed to decode JSON: ' + str(e)}), 500

    file.seek(0)  # Возвращаем указатель обратно в начало файла для последующих операций
    new_photo = Photo(filename=new_filename, content=file.read())
    db.session.add(new_photo)

    # Теперь сохраняем изменения, чтобы получить photo.id
    db.session.commit()
    print("DBAdd Results:", new_photo.id)  # для отладки

    for result in results_json:
        # Проверяем, что результат - это словарь
        if isinstance(result, dict):
            detection_result = DetectionResult(
                photo_id=new_photo.id,
                label=result.get('name'),  # 'name' или 'label' в зависимости от структуры результата
                confidence=result.get('confidence'),
                x_min=result.get('xmin'),
                y_min=result.get('ymin'),
                x_max=result.get('xmax'),
                y_max=result.get('ymax')
            )

            print("Adding Detection Result:", detection_result)  # Для отладки
            db.session.add(detection_result)

    try:
        db.session.commit()
        return jsonify({'message': 'Image uploaded successfully', 'results': results_json}), 201
    except Exception as e:
        db.session.rollback()  # Откат транзакции при ошибке
        print("Error committing to database:", e)  # Для отладки
        return jsonify({'error': str(e)}), 500


@app.route('/photos', methods=['GET'])
def get_photos():
    photos = Photo.query.all()
    results = []
    for photo in photos:
        detection_results = DetectionResult.query.filter_by(photo_id=photo.id).all()
        detected_objects = [{'label': result.label, 'confidence': result.confidence} for result in detection_results]
        results.append({
            'id': photo.id,
            'filename': photo.filename,
            'detected_objects': detected_objects
        })
        print("На мобильный клиент отправлено:", len(results))# Для отладки
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

