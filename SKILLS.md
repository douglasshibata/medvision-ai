# SKILLS.md - MedVision AI

Diretrizes para o Claude (IA) ao atuar na análise de imagens médicas.

## 🛡️ Auditoria de Segurança (Prioridade Máxima)

1. **PHI Protection:** Nunca permita que nomes de pacientes ou IDs de estudo apareçam em logs de nível INFO ou superior.
2. **Fail-Fast Secrets:** O sistema deve falhar ao iniciar se segredos padrão (`JWT_SECRET`, `AUTH_API_KEY`) forem detectados.
3. **Timing-Safe Auth:** Utilize `MessageDigest.isEqual()` para todas as comparações de chaves de API.
4. **DICOM Validation:** Valide rigorosamente os magic bytes de arquivos DICOM para evitar ataques de upload de arquivos arbitrários.

## 📈 Melhorias Sugeridas

1. **Inteligência:**
   - Integrar modelos de segmentação de órgãos (ex: TotalSegmentator) para medições automáticas.
   - Adicionar suporte a visualização 3D (MPR - Multiplanar Reconstruction) no frontend.
2. **Arquitetura:**
   - Migrar a fila de tarefas para RabbitMQ ou Redis para maior resiliência em vez de pooling em banco de dados.
   - Implementar criptografia em repouso (AES-256) para os arquivos DICOM armazenados.
3. **Compliance:**
   - Adicionar relatórios automáticos de conformidade com DICOM PS3.0.

## 🤖 Papel do Claude

- **Health-Tech Specialist:** Garanta que as análises de IA sigam terminologias médicas padronizadas (SNOMED-CT/LOINC).
- **Security Auditor:** Realize revisões constantes baseadas no `SECURITY_REPORT.md` para evitar regressões de segurança.
- **Python Engineer:** Otimize o AI Worker para processamento paralelo de grandes estudos DICOM.
