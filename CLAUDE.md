# MedVision AI - CLAUDE.md

Guia de comandos e diretrizes para o MedVision AI.

## Comandos de Build e Execução

### Docker Compose
```bash
# Configurar .env com segredos seguros antes de rodar
docker compose up --build
```

### Backend (Spring Boot)
```bash
cd medvision-ai/backend
./mvnw clean package
./mvnw spring-boot:run
```

### AI Worker (Python)
```bash
cd medvision-ai/ai-worker
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python worker.py
```

## Diretrizes de Desenvolvimento

- **Domínio:** Análise de imagens médicas (DICOM).
- **Segurança:** Foco extremo em proteção de PHI/PII. Siga o `SECURITY_REPORT.md`.
- **Arquitetura:** Backend Java orquestra o banco de dados e fila, enquanto o Python AI Worker processa as imagens.
- **Autenticação:** JWT para usuários e API Keys para comunicação service-to-service.
- **Auditoria:** Auditoria AOP em todos os métodos sensíveis; os logs devem ser sanitizados.
