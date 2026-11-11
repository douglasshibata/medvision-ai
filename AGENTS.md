# AGENTS.md

Este arquivo fornece instruções e contexto para agentes de IA que trabalham no projeto **MedVision AI**.

## Visão Geral do Projeto
Plataforma de análise de imagens médicas (DICOM) com processamento via IA, focada em proteção de dados de saúde (PHI/PII).

## Tecnologias Utilizadas
- **Backend:** Java (Spring Boot).
- **AI Worker:** Python (processamento de imagem).
- **Domínio:** DICOM (Digital Imaging and Communications in Medicine).
- **Segurança:** JWT, API Keys, Auditoria AOP.

## Estrutura de Pastas
- `backend/`: Orquestração de tarefas e API.
- `ai-worker/`: Worker de processamento de imagens médicas.
- `SECURITY_REPORT.md`: Relatório detalhado de segurança.

## Comandos Principais
- Build/Instalação: `docker compose up --build`
- Backend: `./mvnw spring-boot:run`
- AI Worker: `python worker.py`

## Diretrizes de Desenvolvimento
- **Proteção PHI:** Nomes de pacientes e IDs de estudo nunca devem aparecer em logs.
- **Segurança:** O sistema deve falhar se segredos padrão forem detectados. Use comparações timing-safe para chaves de API.
- **DICOM:** Valide rigorosamente os magic bytes de arquivos DICOM para evitar uploads maliciosos.
- **Integridade:** Siga terminologias médicas padronizadas (SNOMED-CT/LOINC).
