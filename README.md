# Architect AI — Repositório Inicial

Este repositório contém o PRD inicial e um bootstrap mínimo para o desenvolvimento local do MVP do Architect AI.

Arquivos criados nesta etapa:
- `docs/PRD-ArchitectAI.md` — documento PRD inicial
- `infra/docker-compose.yml` — infraestrutura local mínima (Postgres, Redis, MinIO)
- `scripts/run-local.ps1` — script para ambiente Windows PowerShell para subir infra local
- `backend/.gitkeep` — placeholder para código backend
- `frontend/.gitkeep` — placeholder para código frontend

Como rodar local (Windows PowerShell)

1) Subir infraestrutura mínima (Docker must be installed):

```powershell
cd .\infra
docker compose up -d
```

2) Verificar containers:

```powershell
docker ps
```

3) Abrir `docs/PRD-ArchitectAI.md` para revisar requisitos e próximas tarefas.

Próximos passos sugeridos:
- Inicializar o esqueleto do backend (Spring Boot) e frontend (Next.js)
- Implementar Git Connector e wrappers de análise estática
- Criar AI Orchestrator e agentes iniciais (Java Specialist, Tech Lead)

Se desejar, posso criar agora o esqueleto do backend (Spring Boot) e do frontend (Next.js) neste repositório.

