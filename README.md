# Architect AI — MVP Implementation

Enterprise AI-powered Software Architecture & Engineering Consulting Platform.

## Status Atual

✅ **Fase 1 (PRD)** — Documento PRD inicial concluído
✅ **Fase 2 (Backend Skeleton)** — Spring Boot 3 + Java 21 criado
✅ **Fase 3 (Git Connector)** — JGit clone + webhook GitHub implementado
✅ **Fase 4 (Pipeline B2)** — Fila de jobs + orquestração de análises CONCLUÍDO

## Estrutura do Repositório

```
ArchitectAI/
├── docs/
│   └── PRD-ArchitectAI.md ..................... Documento PRD inicial
├── backend/
│   ├── src/main/java/com/architectai/backend/
│   │   ├── model/ ............................ Analysis, Project
│   │   ├── service/ .......................... AnalysisService, ProjectService, GitService, etc.
│   │   └── controller/ ....................... AnalysisController, ProjectController, WebhookController
│   ├── pom.xml .............................. Maven configuration
│   ├── README.md ............................ Backend documentation
│   ├── TEST_PIPELINE.md .................... Testing guide (7 tests)
│   ├── PIPELINE_B2_SUMMARY.md .............. Visual summary
│   └── API_SPECIFICATION_B2.md ............ Formal API spec
├── infra/
│   └── docker-compose.yml .................. Local development stack
├── frontend/
│   └── .gitkeep ........................... Placeholder for Next.js
├── scripts/
│   └── run-local.ps1 ..................... PowerShell script for dev setup
├── CHECKLIST_B2.md ....................... Quality checklist
├── IMPLEMENTATION_B2_COMPLETE.md ......... Executive summary
└── README.md (este arquivo)

```

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+ (ou IDE com suporte Spring Boot)
- Git

### Rodar Backend Localmente

```powershell
cd backend
mvn -DskipTests spring-boot:run
```

Backend inicia em: `http://localhost:8080`

### Testar Pipeline Completo

Ver `backend/TEST_PIPELINE.md` para 7 testes detalhados com PowerShell/curl.

Teste rápido (happy path):
```powershell
# 1. Criar projeto
$p = curl -s -X POST http://localhost:8080/api/v1/projects `
  -H "Content-Type: application/json" `
  -d '{"repoUrl":"https://github.com/spring-projects/spring-petclinic.git","defaultBranch":"main"}' | ConvertFrom-Json

# 2. Criar análise
$a = curl -s -X POST http://localhost:8080/api/v1/projects/$($p.id)/analyses `
  -H "Content-Type: application/json" `
  -d '{"type":"CODE_REVIEW"}' | ConvertFrom-Json

# 3. Monitorar (30 segundos)
for ($i = 0; $i -lt 6; $i++) {
  Start-Sleep 5
  $status = curl -s http://localhost:8080/api/v1/analyses/$($a.id) | ConvertFrom-Json
  Write-Host "[$($i*5)s] Status: $($status.status)"
}
```

## Endpoints Principais (B2)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/v1/projects` | Criar projeto |
| POST | `/api/v1/projects/{projectId}/analyses` | Criar análise (enfileira) |
| GET | `/api/v1/analyses/{id}` | Status de análise |
| GET | `/api/v1/projects/{projectId}/analyses` | Listar análises |
| GET | `/api/v1/queue/size` | Monitoramento da fila |
| POST | `/api/v1/webhooks/github` | Webhook GitHub (HMAC) |

Ver `backend/API_SPECIFICATION_B2.md` para especificação completa.

## Pipeline de Análise (B2)

```
Cliente envia repositório
    ↓
Cria projeto (REST)
    ↓
Inicia análise (enfileira)
    ↓
AnalysisJobProcessor (background)
├─ CLONING: clona via JGit
├─ ANALYZING: executa análises estáticas (TODO)
└─ COMPLETED: gera relatório
    ↓
Cliente consulta status/download
```

Timing esperado: 15-30 segundos (clonagem + análise stub)

## Arquitetura de Alto Nível (B2)

```
┌─────────────────────────────────────┐
│      REST Controllers               │
│  AnalysisController, etc.           │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Service Layer                  │
│  AnalysisService, ProjectService    │
│  GitService, WebhookSecurityUtil    │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│      Data & Queue                   │
│  ConcurrentHashMap (store)          │
│  BlockingQueue<Analysis>            │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│    AnalysisJobProcessor             │
│    (Scheduled task, polling 5s)     │
│    Orquestra pipeline               │
└─────────────────────────────────────┘
```

## Status de Implementação

### Fase 2 (Backend Skeleton) ✅
- [x] Spring Boot 3 + Java 21
- [x] Controllers básicos (Projects, Analyses)
- [x] DTOs e Models

### Fase 3 (Git Connector + Webhook) ✅
- [x] JGit clone (depth=1)
- [x] GitHub webhook
- [x] HMAC-SHA256 validation
- [x] OAuth integration (stub)

### Fase 4 (Pipeline B2) ✅
- [x] Fila de jobs (BlockingQueue)
- [x] Job Processor (scheduled task)
- [x] Transições de estado (PENDING → CLONING → ANALYZING → COMPLETED)
- [x] Auto-create Analysis no webhook
- [x] Endpoints de monitoramento

### Próximos (B3, C, D, E)
- [ ] Static Analysis Engine (SpotBugs, PMD, Checkstyle, Semgrep) — **B3**
- [ ] Containerização (Docker, docker-compose) — **C**
- [ ] Persistência (PostgreSQL + Spring Data JPA) — **D**
- [ ] Frontend (Next.js + React) — **E**
- [ ] AI Orchestrator (Spring AI + Agents) — **F**

## Documentação

- **docs/PRD-ArchitectAI.md** — Documento PRD completo (visão, produtos, arquitetura)
- **backend/README.md** — Backend setup + endpoints principais
- **backend/TEST_PIPELINE.md** — 7 testes manuais com exemplos PowerShell/curl
- **backend/API_SPECIFICATION_B2.md** — Especificação formal com exemplos JSON
- **backend/PIPELINE_B2_SUMMARY.md** — Resumo visual do fluxo
- **IMPLEMENTATION_B2_COMPLETE.md** — Documento executivo
- **CHECKLIST_B2.md** — Checklist de qualidade

## Configuração

### Backend (application.yml)
```yaml
spring:
  application:
    name: architect-ai-backend
  profiles:
    active: dev

github:
  webhook:
    secret: ${GITHUB_WEBHOOK_SECRET:dev-secret-32-chars-min}

architectai:
  runtime:
    workspace-dir: ${ARCHITECTAI_RUNTIME_WORKSPACE_DIR:./workspace}
    reports-dir: ${ARCHITECTAI_RUNTIME_REPORTS_DIR:./.architectai/reports}

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### Variáveis de Ambiente
```powershell
$env:GITHUB_WEBHOOK_SECRET = "seu-secret-aqui"
$env:ARCHITECTAI_RUNTIME_WORKSPACE_DIR = "./workspace"
$env:ARCHITECTAI_RUNTIME_REPORTS_DIR = "./.architectai/reports"
```

## Performance (MVP)

| Métrica | Valor | Notas |
|---------|-------|-------|
| POST /projects | < 100ms | In-memory, sem I/O |
| POST /analyses | < 100ms | Apenas enfileira |
| Clone (depth=1) | 10-20s | Depende de conexão |
| Total pipeline | 15-30s | Clone + análise stub |
| Throughput | Sem limite | In-memory apenas |

Para SaaS: trocar por PostgreSQL + Kafka

## Dependências Principais

- Spring Boot 3.1.6
- Java 21
- JGit 6.8.0
- Jackson (JSON)
- Maven

Zero dependências adicionadas para B2 ✅

## Próximos Passos Recomendados

### B3 — Static Analysis Engine (1 semana)
Implementar pipeline:
- Wrappers para SpotBugs, PMD, Checkstyle, Semgrep
- Parser de outputs estruturados
- Consolidação de findings

### C — Containerização (3 dias)
Preparar deployment:
- Dockerfile
- docker-compose atualizado
- CI/CD (GitHub Actions)

### D — Persistência (5 dias)
Trocar para banco real:
- PostgreSQL + Spring Data JPA
- Flyway migrations
- Testes de integridade

### E — Frontend (2 semanas)
Criar UI:
- Next.js + React
- Dashboard de análises
- Upload de repositório
- Download de relatórios

## Contribuindo

1. Branch: `feature/...`
2. Tests: `backend/TEST_PIPELINE.md`
3. Docs: Atualizar README.md e arquivos .md relevantes
4. PR: Referência para issue

## Contacto & Suporte

Para dúvidas ou melhorias, abrir issue ou PR no repositório.

---

**Última atualização**: 2026-08-04
**Status**: MVP Ready (B2 Concluído)
**Próximo**: B3 — Static Analysis Engine
