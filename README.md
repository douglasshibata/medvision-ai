# MedVision AI

MedVision AI is a medical imaging platform (Radiology Assistant) designed for high-performance DICOM analysis and secure data management.

## 📋 Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Security & PHI Protection](#security--phi-protection)

## Overview
The platform provides tools for medical professionals to upload, view, and analyze DICOM images. It uses specialized AI models (BiomedCLIP/PyTorch) to assist in radiological assessments while maintaining strict compliance with health data protection standards.

## Architecture
- **Microservices-lite**: Java Spring Boot backend for orchestration and a Python AI-Worker for heavy image processing.
- **DICOM Handling**: Validates DICOM magic bytes and processes metadata securely.
- **Asynchronous Pipeline**: Uses Redis and AOP (Aspect-Oriented Programming) for auditing and task management.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.4.3.
- **AI Engine**: Python 3.12, PyTorch, BiomedCLIP.
- **Frontend**: HTML5/JavaScript, Tailwind CSS, Cornerstone.js for DICOM visualization.
- **Persistence & Audit**: PostgreSQL, Redis, AOP for audit logging.

## Getting Started

### Prerequisites
- Java 21
- Python 3.12
- Docker

### Installation
Run the entire stack using Docker Compose:
```bash
docker compose up --build
```

Alternatively, run components individually:
- **Backend**: `cd backend && ./mvnw spring-boot:run`
- **AI Worker**: `cd ai-worker && python worker.py`

## Project Structure
- `backend/`: Java Spring Boot API and orchestration.
- `ai-worker/`: Python service for AI model inference.
- `frontend/`: Web-based DICOM viewer and interface.
- `AGENTS.md`: Development guidelines for AI assistants.
- `SECURITY_REPORT.md`: Security analysis and findings.

## Security & PHI Protection
- **Strict PHI Exclusion**: Patient names and Study UIDs are never logged.
- **Secure Authentication**: JWT-based auth with rate-limiting.
- **Fail-Fast Policy**: System rejects startup with default secrets.
- **Audit Logging**: Comprehensive auditing via Spring AOP.

## License
Medical Professional / Enterprise License

---

## 🛠️ Project Deep Dive & Technical Guide

### 🎯 Main Purpose
Medical Imaging (DICOM) Analysis and diagnostic assist platform prioritizing strict PHI/PII protection.

### 🧰 Tecnologias Utilizadas (Tech Stack)
- **Primary Technologies:** Docker / Docker Compose, FastAPI, Java 21, Maven, Python 3.12, Spring Boot 3.4.3
- **Deployment Strategy:** Container-ready with standard Docker/Docker Compose integrations where applicable.

### 🚀 Como Implantar e Executar (How to Deploy & Run)
#### Backend (Java / Spring Boot)
1. Navigate to the backend directory.
2. Compile and run: `./mvnw spring-boot:run` or `mvn spring-boot:run`.

#### Python Engine / FastAPI
1. Install dependencies: `pip install -r requirements.txt`.
2. Run the server/script: `uvicorn main:app --reload` or `python main.py`.

---
