# Architect AI — Especificação de API B2

## Resumo das Mudanças (B2)

B2 conectou o Git Connector (B1) ao pipeline de análise com fila de jobs, orquestração de status e webhook automático.

### Componentes Adicionados

1. **ProjectService** — CRUD de projetos em-memory
2. **AnalysisService** — orquestração do ciclo de vida com fila
3. **AnalysisJobProcessor** — consumer de fila (scheduled task, polling 5s)
4. **Integração WebhookController** — auto-criar projetos e análises

### Modelo de Dados — Analysis

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "projectId": "p1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "type": "CODE_REVIEW",
  "status": "COMPLETED",
  "createdAt": "2026-08-04T10:00:00",
  "startedAt": "2026-08-04T10:00:05",
  "finishedAt": "2026-08-04T10:00:35",
  "agentVersion": "1.0.0",
  "repositoryPath": "/workspace/a1b2c3d4-8a9f",
  "errorMessage": null,
  "findingsCount": 42,
  "reportUrl": "s3://architect-ai-reports/a1b2c3d4-e5f6.pdf",
  "estimatedCostRs": 2000
}
```

### Transições de Estado

```
PENDING
   │
   ├─(enfileirado)────────────────────────┐
   │                                       │
   ▼                                       │
CLONING                            (falha na clonagem)
   │                                       │
   ├─(clonagem OK)  ◄────────────────────┘
   │
   ▼
ANALYZING
   │
   ├─(análise OK)──────────────┐
   │                           │
   ▼                           ▼
COMPLETED               FAILED
   │                      │
   └──(relatório OK)      └──(error message)
```

## Especificação Técnica — Endpoints (B2)

### Projects (existentes, sem mudanças)

**POST /api/v1/projects**

Request:
```json
{
  "repoUrl": "https://github.com/user/repo.git",
  "defaultBranch": "main"
}
```

Response (201 Created):
```json
{
  "id": "proj-uuid",
  "repoUrl": "https://github.com/user/repo.git",
  "defaultBranch": "main",
  "createdAt": "2026-08-04T10:00:00"
}
```

**GET /api/v1/projects/{id}**

Response (200):
```json
{
  "id": "proj-uuid",
  "repoUrl": "https://github.com/user/repo.git",
  "defaultBranch": "main",
  "createdAt": "2026-08-04T10:00:00"
}
```

### Analyses (B2 — novos endpoints)

**POST /api/v1/projects/{projectId}/analyses**

Request:
```json
{
  "type": "CODE_REVIEW"
}
```

Tipos aceitos:
- `CODE_REVIEW` — análise de código e padrões
- `ARCHITECTURE` — análise de estrutura e modularização
- `PERFORMANCE` — análise de gargalos e otimização
- `SECURITY` — análise de vulnerabilidades (OWASP)
- `CLOUD` — análise de infra na nuvem
- `DATABASE` — análise de queries e índices
- `DUE_DILIGENCE` — análise para investidores

Response (201 Created):
```json
{
  "id": "ana-uuid",
  "projectId": "proj-uuid",
  "type": "CODE_REVIEW",
  "status": "PENDING",
  "createdAt": "2026-08-04T10:00:00",
  "startedAt": null,
  "finishedAt": null,
  "agentVersion": null,
  "repositoryPath": null,
  "errorMessage": null,
  "findingsCount": 0,
  "reportUrl": null,
  "estimatedCostRs": null
}
```

**GET /api/v1/analyses/{id}**

Response (200):
```json
{
  "id": "ana-uuid",
  "projectId": "proj-uuid",
  "type": "CODE_REVIEW",
  "status": "CLONING",
  "createdAt": "2026-08-04T10:00:00",
  "startedAt": "2026-08-04T10:00:01",
  "finishedAt": null,
  "agentVersion": "1.0.0",
  "repositoryPath": null,
  "errorMessage": null,
  "findingsCount": 0,
  "reportUrl": null,
  "estimatedCostRs": null
}
```

Status possíveis:
- `PENDING` — enfileirada, aguardando processamento
- `CLONING` — repositório sendo clonado
- `ANALYZING` — em análise (futuro: pipeline de tools)
- `COMPLETED` — concluída com sucesso
- `FAILED` — erro durante processamento

Respostas de erro:
- 404 Not Found — análise não existe
- 400 Bad Request — projeto não encontrado

**GET /api/v1/projects/{projectId}/analyses**

Response (200):
```json
[
  {
    "id": "ana-1",
    "projectId": "proj-uuid",
    "type": "CODE_REVIEW",
    "status": "COMPLETED",
    "createdAt": "2026-08-04T10:00:00"
  },
  {
    "id": "ana-2",
    "projectId": "proj-uuid",
    "type": "ARCHITECTURE",
    "status": "PENDING",
    "createdAt": "2026-08-04T11:00:00"
  }
]
```

**GET /api/v1/analyses/{id}/report**

Response quando `status = COMPLETED` (200):
```text
Report URL: s3://architect-ai-reports/ana-uuid.pdf
```

Response quando `status = FAILED` (400):
```text
Analysis failed: Connection timeout while cloning repository
```

Response quando ainda processando (202 Accepted):
```text
Analysis is still processing. Status: ANALYZING
```

**GET /api/v1/queue/size**

Response (200):
```json
3
```

Retorna número de análises enfileiradas aguardando processamento.

### Webhooks (B2 — integrado com fila)

**POST /api/v1/webhooks/github**

Headers obrigatórios:
- `Content-Type: application/json`
- `X-Hub-Signature-256: sha256={hexdigest}` — validação HMAC-SHA256

Request (estrutura GitHub push event):
```json
{
  "ref": "refs/heads/main",
  "repository": {
    "clone_url": "https://github.com/user/repo.git",
    "name": "repo"
  }
}
```

Response (200 OK):
```json
{
  "message": "Webhook processed successfully",
  "projectId": "proj-uuid-novo",
  "analysisId": "ana-uuid-novo",
  "status": "PENDING"
}
```

Erros:
- 400 Bad Request — assinatura inválida ou payload malformado
- 500 Internal Server Error — erro ao processar webhook

## Sequência de Operações — Happy Path

```
1. Cliente envia: POST /api/v1/projects
   └─ Retorna: Project{id, repoUrl, defaultBranch, createdAt}

2. Cliente envia: POST /api/v1/projects/{id}/analyses
   └─ Retorna: Analysis{id, status:"PENDING", createdAt}
   └─ Side-effect: enfileira em analysisQueue

3. AnalysisJobProcessor.processAnalysisQueue() (a cada 5s)
   └─ Desfileira Analysis
   └─ Muda status → CLONING
   └─ Chama GitService.cloneRepositoryForAnalysis()
   └─ Muda status → ANALYZING
   └─ (TODO) Executa SpotBugs, PMD, etc.
   └─ Muda status → COMPLETED (ou FAILED)
   └─ Salva metadados: repositoryPath, findingsCount, reportUrl, estimatedCostRs

4. Cliente consulta: GET /api/v1/analyses/{id}
   └─ Retorna: Analysis{status:"COMPLETED", repositoryPath, findingsCount, reportUrl}

5. Cliente baixa: GET /api/v1/analyses/{id}/report
   └─ Retorna: URL ou erro
```

## Sequência de Operações — Webhook Flow

```
1. GitHub envia: POST /api/v1/webhooks/github
   └─ Header: X-Hub-Signature-256: sha256={hmac}
   └─ Body: {ref, repository}

2. WebhookController.githubWebhook()
   └─ Valida assinatura (WebhookSecurityUtil.isValidSignature)
   └─ Parse JSON
   └─ Cria Project (ProjectService.createProject)
   └─ Cria Analysis (AnalysisService.createAnalysis)
   └─ Enfileira

3. Retorna: {message, projectId, analysisId, status:"PENDING"}

4. (Fluxo continua igual ao acima, steps 3-5)
```

## Diagramas C4 (Context)

### C1 — System Context

```
┌─────────────────┐
│  GitHub User    │
│  (Developer)    │
└────────┬────────┘
         │ push event
         ▼
┌─────────────────────────────────────────────────────┐
│         GitHub                                       │
│  (remote repository)                                 │
└────────┬──────────────────────────────────────────────┘
         │ webhook
         ▼
┌──────────────────────────────────────────────────────┐
│     Architect AI Backend (Spring Boot)               │
│  - Orquestra análises                                │
│  - Executa pipeline                                  │
│  - Gera relatórios                                   │
└────────┬───────────────────────────────┬──────────────┘
         │                               │
         ▼                               ▼
    ┌─────────┐                  ┌──────────────┐
    │ Workspace                  │ S3 Storage   │
    │ (local)  │                 │ (reports)    │
    └──────────┘                 └──────────────┘
```

### C2 — Container (Backend internals)

```
┌─────────────────────────────────────────────────────────┐
│         Spring Boot Backend Container                    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         REST API Controllers                     │  │
│  │  - ProjectController                            │  │
│  │  - AnalysisController                           │  │
│  │  - WebhookController                            │  │
│  └────────────┬──────────────────┬─────────────────┘  │
│               │                  │                     │
│               ▼                  ▼                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │      Service Layer                              │  │
│  │  - ProjectService                               │  │
│  │  - AnalysisService (enfileira)                   │  │
│  │  - GitService (JGit clone)                       │  │
│  │  - WebhookSecurityUtil (HMAC validation)        │  │
│  └────────────┬──────────────────┬─────────────────┘  │
│               │                  │                     │
│               ▼                  ▼                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │     Data/Queue Layer                            │  │
│  │  - ConcurrentHashMap (projects, analyses)       │  │
│  │  - BlockingQueue<Analysis> (job queue)          │  │
│  └──────────┬───────────────────────────────────────┘  │
│             │                                           │
│             ▼                                           │
│  ┌──────────────────────────────────────────────────┐  │
│  │    Background Processor                         │  │
│  │  - AnalysisJobProcessor (Scheduled task)        │  │
│  │    Polling queue a cada 5s                      │  │
│  │    Orquestra pipeline                           │  │
│  └──────────────────────────────────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Timing esperado (MVP)

| Operação | Tempo | Notas |
|----------|-------|-------|
| POST /projects | < 100ms | em-memory, nenhuma I/O |
| POST /analyses | < 100ms | apenas enfileira, não processa |
| Polling (queue poll) | 5s | intervalo fixo |
| Clonagem (spring-petclinic ~150MB depth=1) | ~10-20s | depende de conexão internet |
| Static Analysis (TODO) | TBD | não implementado ainda |
| Total pipeline (até COMPLETED) | ~15-25s | clonagem + análise stub |

## Configuração (application.yml)

```yaml
spring:
  application:
    name: architect-ai-backend
  profiles:
    active: dev

github:
  webhook:
    secret: ${GITHUB_WEBHOOK_SECRET:dev-secret-minimo-32-caracteres}

# Logging
logging:
  level:
    com.architectai.backend: INFO
    org.eclipse.jgit: WARN
```

Variáveis de ambiente:
- `GITHUB_WEBHOOK_SECRET` — Secret HMAC para webhook (obrigatório em produção)

## Próximos Passos

- **B3**: Implementar Static Analysis Engine (SpotBugs, PMD, Checkstyle, Semgrep)
- **B4**: AI Orchestrator e Agentes especializados (Spring AI)
- **C**: Containerização (Docker, docker-compose)
- **D**: Persistência (PostgreSQL + Spring Data JPA)
- **E**: Frontend (Next.js + React)

