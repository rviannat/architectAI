# Backend — Architect AI

Este diretório contém o esqueleto do backend em Spring Boot (Java 21).

Como rodar (requisitos: Java 21, Maven):

```powershell
cd backend
mvn -DskipTests spring-boot:run
```

Endpoints disponíveis (in-memory stub):
- POST /api/v1/projects — criar projeto (body: {"repoUrl":"git@...","defaultBranch":"main"})
- GET /api/v1/projects/{id} — obter projeto
- POST /api/v1/projects/{id}/analyses — iniciar análise (body: {"type":"CODE_REVIEW"})
- GET /api/v1/analyses/{id} — consultar status
- GET /api/v1/analyses/{id}/report — obter relatório (placeholder)
- POST /api/v1/projects/{id}/clone — clona o repositório do projeto (header optional: X-Git-Token)
- POST /api/v1/webhooks/github — webhook GitHub (recebe push events e clona o repositório). Header optional: X-Git-Token

Este skeleton usa armazenamento em memória para facilitar o desenvolvimento do MVP. Próximos passos: integrar Git Connector e pipeline de análise.

