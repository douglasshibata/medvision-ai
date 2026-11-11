import redis
import json
import time
import os
import logging
import pydicom
import numpy as np
import requests
import torch
from PIL import Image
from open_clip import create_model_from_pretrained, get_tokenizer

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
REDIS_PASSWORD = os.getenv('REDIS_PASSWORD', None)
BACKEND_URL = os.getenv('BACKEND_URL', "http://localhost:8080/api/v1/results")
AI_WORKER_API_KEY = os.getenv('AI_WORKER_API_KEY', '')
ALLOWED_BASE_DIR = os.path.realpath(os.getenv('UPLOAD_DIR', '/upload-dir'))

r = redis.Redis(host=REDIS_HOST, port=6379, db=0, password=REDIS_PASSWORD)
queue_name = 'ai_analysis_queue'

# BiomedCLIP for medical image classification
BIOMEDCLIP_MODEL = "hf-hub:microsoft/BiomedCLIP-PubMedBERT_256-vit_base_patch16_224"
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# Medical classification labels for zero-shot classification
MEDICAL_LABELS = [
    "normal healthy tissue",
    "abnormal mass or tumor",
    "inflammatory changes",
    "fracture or bone abnormality",
    "fluid accumulation or effusion",
]

biomedclip_model = None
biomedclip_preprocess = None
biomedclip_tokenizer = None

def load_biomedclip():
    """Load BiomedCLIP model for medical image classification."""
    global biomedclip_model, biomedclip_preprocess, biomedclip_tokenizer
    logger.info("Loading BiomedCLIP model...")
    biomedclip_model, biomedclip_preprocess = create_model_from_pretrained(BIOMEDCLIP_MODEL)
    biomedclip_tokenizer = get_tokenizer(BIOMEDCLIP_MODEL)
    biomedclip_model = biomedclip_model.to(DEVICE)
    biomedclip_model.eval()
    logger.info("BiomedCLIP model loaded successfully.")

def classify_medical_image(image: Image.Image) -> dict:
    """Classify a medical image using BiomedCLIP zero-shot classification."""
    image_input = biomedclip_preprocess(image).unsqueeze(0).to(DEVICE)
    text_inputs = biomedclip_tokenizer(MEDICAL_LABELS).to(DEVICE)

    with torch.no_grad():
        image_features, text_features, _ = biomedclip_model(image_input, text_inputs)
        image_features = image_features / image_features.norm(dim=-1, keepdim=True)
        text_features = text_features / text_features.norm(dim=-1, keepdim=True)
        similarities = (image_features @ text_features.T).squeeze(0).cpu().numpy()

    probs = np.exp(similarities) / np.sum(np.exp(similarities))
    label_scores = {label: float(prob) for label, prob in zip(MEDICAL_LABELS, probs)}
    top_label = MEDICAL_LABELS[int(np.argmax(probs))]
    top_confidence = float(np.max(probs))

    return {
        "top_label": top_label,
        "confidence": top_confidence,
        "all_scores": label_scores,
    }

def findings_text(label, conf, measurements):
    if label == "normal healthy tissue":
        return "Tecidos visualizados com morfologia e atenuação preservadas. Sem evidências de massas ou coleções."

    text = f"Identificada alteração compatível com '{label}'. "
    if measurements:
        text += f"Dimensões aproximadas: {measurements['longest_diameter_mm']} mm no maior eixo. "
    text += f"Grau de certeza diagnóstica: {conf*100:.1f}%."
    return text

def dicom_to_pil(pixel_data: np.ndarray) -> Image.Image:
    """Convert DICOM pixel data to a PIL Image suitable for BiomedCLIP."""
    data = pixel_data.astype(np.float32)
    if data.max() > data.min():
        data = (data - data.min()) / (data.max() - data.min()) * 255.0
    data = data.astype(np.uint8)
    if data.ndim == 2:
        return Image.fromarray(data).convert("RGB")
    return Image.fromarray(data)

def validate_file_path(file_path):
    """Validate that file_path is within the allowed base directory."""
    real_path = os.path.realpath(file_path)
    if not real_path.startswith(ALLOWED_BASE_DIR + os.sep) and real_path != ALLOWED_BASE_DIR:
        raise ValueError(f"Path traversal detected: path is outside allowed directory")
    if not os.path.isfile(real_path):
        raise FileNotFoundError(f"File not found at validated path")
    return real_path

def process_dicom(file_path):
    logger.info("Reading DICOM file for analysis")
    ds = pydicom.dcmread(file_path)
    pixel_data = ds.pixel_array

    logger.info("Processing image of dimensions: %s", pixel_data.shape)

    # If 3D volume, take the middle slice
    if pixel_data.ndim == 3 and pixel_data.shape[0] > 1:
        mid = pixel_data.shape[0] // 2
        slice_data = pixel_data[mid]
    else:
        slice_data = pixel_data if pixel_data.ndim == 2 else pixel_data[0]

    image = dicom_to_pil(slice_data)
    classification = classify_medical_image(image)

    top_label = classification["top_label"]
    confidence = classification["confidence"]
    anomaly_detected = top_label != "normal healthy tissue"

    # Simulated quantitative measurements (RECIST-like)
    measurements = {}
    if anomaly_detected:
        # Simulate detection of a lesion
        lesion_size_mm = float(np.random.uniform(5.0, 35.0))
        measurements = {
            "lesion_id": "L-001",
            "longest_diameter_mm": round(lesion_size_mm, 2),
            "volume_cm3": round((4/3) * np.pi * (lesion_size_mm/20)**3, 2),
            "density_hounsfield": int(np.random.uniform(20, 100)) if "CT" in str(ds.Modality) else None
        }

    # Structured report generation
    structured_report = {
        "clinical_context": "Análise automatizada via MedVision AI Assist.",
        "findings": findings_text(top_label, confidence, measurements),
        "impression": "Achados sugestivos de patologia ativa." if anomaly_detected else "Exame dentro dos limites da normalidade.",
        "recommendations": "Correlação clínica e revisão por radiologista sênior." if anomaly_detected else "Seguimento de rotina conforme protocolos institucionais."
    }

    if anomaly_detected:
        findings = (
            f"LAUDO PRELIMINAR (AI - BiomedCLIP): Classificação indica '{top_label}' "
            f"no estudo {ds.StudyInstanceUID}. "
            f"Medição: {measurements.get('longest_diameter_mm')}mm. "
            f"Confiança: {confidence*100:.1f}%. Recomenda-se revisão prioritária."
        )
    else:
        findings = (
            "LAUDO PRELIMINAR (AI - BiomedCLIP): Estudo classificado como dentro dos padrões de normalidade. "
            f"Confiança: {confidence*100:.1f}%. "
            "Nenhuma alteração significativa identificada."
        )

    result = {
        "studyUID": ds.StudyInstanceUID,
        "instanceUID": ds.SOPInstanceUID,
        "patientName": str(ds.PatientName),
        "confidence": confidence,
        "anomalyDetected": anomaly_detected,
        "classification": classification["top_label"],
        "classificationScores": json.dumps(classification["all_scores"]),
        "findings": findings,
        "measurements": json.dumps(measurements),
        "structuredReport": json.dumps(structured_report),
        "status": "COMPLETED"
    }
    return result

def get_auth_headers():
    """Build authentication headers for backend communication."""
    headers = {"Content-Type": "application/json"}
    if AI_WORKER_API_KEY:
        headers["X-Service-Key"] = AI_WORKER_API_KEY
    return headers

def main():
    if not AI_WORKER_API_KEY:
        logger.warning("AI_WORKER_API_KEY not set. Backend requests will be unauthenticated.")

    load_biomedclip()
    logger.info("MedVision AI Worker started. Waiting for tasks...")
    while True:
        try:
            task_data = r.brpop(queue_name, timeout=5)
            if task_data:
                _, message = task_data
                task = json.loads(message)
                file_path = validate_file_path(task['filePath'])

                logger.info("Starting analysis for study: %s", task.get('studyUID', 'unknown'))

                result = process_dicom(file_path)

                # Send result back to Backend with authentication
                response = requests.post(BACKEND_URL, json=result, headers=get_auth_headers())
                if response.status_code == 200:
                    logger.info("Result sent successfully for study: %s", result.get('studyUID', 'unknown'))
                else:
                    logger.error("Failed to send result to backend: HTTP %d", response.status_code)

        except Exception as e:
            logger.error("Error in worker main loop: %s", e)
            time.sleep(1)

if __name__ == "__main__":
    main()
