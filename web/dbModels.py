from datetime import datetime

from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), nullable=False, unique=True)
    email = db.Column(db.String(120), nullable=False, unique=True)
    password_hash = db.Column(db.String(128))


class Photo(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    filename = db.Column(db.String, nullable=False)
    content = db.Column(db.LargeBinary, nullable=False)
    created_at = db.Column(db.DateTime, default=db.func.current_timestamp())
    shelf_id = db.Column(db.String, nullable=False)
    compliance_checks = db.relationship('ComplianceCheckResult', backref='photo', lazy=True)
    embeddings = db.relationship('Embedding', backref='photo', lazy=True)


class DetectionResult(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    photo_id = db.Column(db.Integer, db.ForeignKey('photo.id'), nullable=False)
    label = db.Column(db.String, nullable=False)
    confidence = db.Column(db.Float, nullable=False)
    x_min = db.Column(db.Float, nullable=False)
    y_min = db.Column(db.Float, nullable=False)
    x_max = db.Column(db.Float, nullable=False)
    y_max = db.Column(db.Float, nullable=False)
    created_at = db.Column(db.DateTime, default=db.func.current_timestamp())

    # Связь с моделью Photo
    photo = db.relationship('Photo', backref=db.backref('detection_results', lazy=True))

# Модель для планограммы
class Planogram(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    shelf_id = db.Column(db.String, nullable=False)
    sku = db.Column(db.String, nullable=False)
    x_min = db.Column(db.Float, nullable=False)
    y_min = db.Column(db.Float, nullable=False)
    x_max = db.Column(db.Float, nullable=False)
    y_max = db.Column(db.Float, nullable=False)
    quantity = db.Column(db.Integer, nullable=False)
    created_at = db.Column(db.DateTime, default=db.func.current_timestamp())

# Модель для результатов сверки
class ComplianceCheckResult(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    photo_id = db.Column(db.Integer, db.ForeignKey('photo.id'), nullable=False)
    sku = db.Column(db.String, nullable=False)
    missing_count = db.Column(db.Integer, nullable=False)
    extra_count = db.Column(db.Integer, nullable=False)
    created_at = db.Column(db.DateTime, default=db.func.current_timestamp())


class Embedding(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    photo_id = db.Column(db.Integer, db.ForeignKey('photo.id'), nullable=False)
    features = db.Column(db.LargeBinary, nullable=False) 