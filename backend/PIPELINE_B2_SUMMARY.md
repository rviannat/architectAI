# Pipeline B2 Concluído — Git Connector + Fila de Análises

## O que foi implementado (B2)

### 1. Modelo Analysis Expandido
- Status completo: PENDING → CLONING → ANALYZING → COMPLETED/FAILED
- Timestamps: createdAt, startedAt, finishedAt
- Metadata: repositoryPath, errorMessage, findingsCount, reportUrl, estimatedCostRs

### 2. AnalysisService
- Orquestra ciclo de vida completo
- Enfileira jobs em `BlockingQueue<Analysis>` (thread-safe, in-memory)
- Gerencia estados e transições
- Métodos:
  - `createAnalysis()` — cria e enfileira
  - `getAnalysis()` — consulta status
  - `listAnalysisByProject()` — lista por projeto
  - `updateAnalysisStatus()` — transições de estado
  - `completeAnalysis()` — finaliza com sucesso
  - `failAnalysis()` — marca como falha

### 3. ProjectService
- CRUD de projetos (em-memory store)
- Armazena repoUrl, defaultBranch, timestamps

### 4. AnalysisJobProcessor
- Scheduled task (polling a cada 5 segundos)
- Desfileira análises da fila
- Orquestra pipeline:
  - Step 1: CLONING (chama GitService)
  - Step 2: ANALYZING (TODO: static analysis engine)
  - Step 3: COMPLETED (com metadados: path, findings, reportUrl, custo estimado)
- Logs estruturados para monitoramento

### 5. GitService Expandido
- Novo método `cloneRepositoryForAnalysis(projectId)`
- Compatível com fluxo de pipeline

### 6. AnalysisController Expandido
- POST `/api/v1/projects/{projectId}/analyses` — cria análise
- GET `/api/v1/analyses/{id}` — status completo
- GET `/api/v1/projects/{projectId}/analyses` — lista análises do projeto
- GET `/api/v1/analyses/{id}/report` — download (placeholder)
- GET `/api/v1/queue/size` — monitoramento da fila

### 7. WebhookController Integrado
- Valida assinatura HMAC (B1 anterior)
- Cria projeto automaticamente
- Enfileira análise automaticamente
- Response inclui projectId + analysisId

## Fluxo Visual (ASCII)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CLIENTE (API ou Webhook)                          │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                ┌──────────┴──────────┐
                │                     │
        POST /projects         POST /webhooks/github
                │                     │
        ┌───────▼────────┐   ┌────────▼─────────┐
        │ Create Project │   │ Webhook Handler  │
        │   (in memory)  │   │ + HMAC Validation│
        └────────┬───────┘   └────────┬─────────┘
                 │                    │
        ┌────────▼────────┐   ┌───────▼────────┐
        │  Store Project  │   │ Create Project │
        │ (ProjectService)│   │ Automatically  │
        └────────┬────────┘   └────────┬───────┘
                 │                     │
        POST /projects/{id}/analyses   │
                 │                     │
        ┌────────▼──────────────────────┴──────┐
        │  AnalysisService.createAnalysis()    │
        │  - Cria Analysis(PENDING)            │
        │  - Enfileira em BlockingQueue        │
        └────────┬────────────────────────────┘
                 │
        ┌────────▼───────────────────────────────┐
        │   analysisQueue.put(analysis)          │
        │   (BlockingQueue em memória)           │
        └────────┬───────────────────────────────┘
                 │
                 │ Response 201: Analysis{id, status:"PENDING", ...}
                 │
        GET /analyses/{id} (cliente consulta periodicamente)
                 │
        ┌────────▼─────────────────────────────────┐
        │  AnalysisJobProcessor (scheduled task)   │
        │  Polling a cada 5 segundos               │
        └────────┬──────────────────────────────────┘
                 │
        ┌────────▼──────────────────────────┐
        │ analysisQueue.poll()               │
        │ (aguarda job disponível)           │
        └────────┬───────────────────────────┘
                 │
        ┌────────▼──────────────────────────────────┐
        │ updateStatus(CLONING)                    │
        └────────┬───────────────────────────────────┘
                 │
        ┌────────▼────────────────────────────────┐
        │ GitService.cloneRepository()            │
        │ - JGit clone depth=1                    │
        │ - Salva em ./workspace/{id}-{timestamp} │
        └────────┬─────────────────────────────────┘
                 │
        ┌────────▼──────────────────────────────────┐
        │ updateStatus(ANALYZING)                  │
        │ (TODO: execute SpotBugs, PMD, etc.)      │
        └────────┬────────────────────────────────────┘
                 │
        ┌────────▼──────────────────────────────────┐
        │ completeAnalysis()                       │
        │ - status = COMPLETED                     │
        │ - Salva path, findings, reportUrl, custo │
        │ - finishedAt = now()                     │
        └────────┬────────────────────────────────────┘
                 │
        GET /analyses/{id}
                 │
        ┌────────▼─────────────────────────────┐
        │ Analysis {                           │
        │   status: "COMPLETED",               │
        │   repositoryPath: "./workspace/...", │
        │   findingsCount: 42,                 │
        │   reportUrl: "s3://reports/xxx.pdf" │
        │   estimatedCostRs: 2000              │
        │ }                                    │
        └──────────────────────────────────────┘
```

## Teste Rápido (PowerShell)

```powershell
# Terminal 1: Rodar backend
cd C:\Users\User\Documents\repositorio\architectAI\ArchitectAI\backend
# (instalar Maven se necessário, ou usar IDE)

# Terminal 2: Testar pipeline
$p = curl -s -X POST http://localhost:8080/api/v1/projects -H "Content-Type: application/json" `
  -d '{"repoUrl":"https://github.com/spring-projects/spring-petclinic.git","defaultBranch":"main"}' | ConvertFrom-Json
$pId = $p.id

$a = curl -s -X POST http://localhost:8080/api/v1/projects/$pId/analyses -H "Content-Type: application/json" `
  -d '{"type":"CODE_REVIEW"}' | ConvertFrom-Json
$aId = $a.id

# Monitorar transição de status (executar múltiplas vezes)
for ($i = 0; $i -lt 10; $i++) {
  Start-Sleep 5
  $status = curl -s http://localhost:8080/api/v1/analyses/$aId | ConvertFrom-Json
  Write-Host "[$($i*5)s] Status: $($status.status)"
  if ($status.status -eq "COMPLETED" -or $status.status -eq "FAILED") { break }
}
```

## Arquivos Criados/Modificados

Criados:
- `backend/src/main/java/com/architectai/backend/service/ProjectService.java`
- `backend/src/main/java/com/architectai/backend/service/AnalysisJobProcessor.java`
- `backend/TEST_PIPELINE.md` — guia de testes

Modificados:
- `backend/src/main/java/com/architectai/backend/model/Analysis.java` — expand fields
- `backend/src/main/java/com/architectai/backend/service/AnalysisService.java` — queue + orchestration
- `backend/src/main/java/com/architectai/backend/service/GitService.java` — add cloneRepositoryForAnalysis()
- `backend/src/main/java/com/architectai/backend/controller/AnalysisController.java` — new endpoints
- `backend/src/main/java/com/architectai/backend/controller/WebhookController.java` — auto-create Analysis
- `backend/README.md` — complete documentation

## Status Atual

✅ Fila de jobs implementada (BlockingQueue)
✅ Processador de jobs (AnalysisJobProcessor)
✅ Transições de estado (PENDING → CLONING → ANALYZING → COMPLETED/FAILED)
✅ Orquestração de pipeline
✅ Clonagem de repositório integrada
✅ Endpoints de monitoramento
✅ Webhook integrado com fila

🔲 Next: B3 — Static Analysis Engine (SpotBugs, PMD, Checkstyle)

## Próximos Passos

Escolha uma opção:

**B3** — Implementar Static Analysis Engine (SpotBugs, PMD, Checkstyle, Semgrep)
  - Wrappers para executar cada ferramenta via CLI
  - Parser de outputs em formato estruturado
  - Consolidação de findings

**C** — Containerizar backend
  - Dockerfile
  - docker-compose.yml atualizado
  - Scripts de build/deploy

Qual quer fazer agora?

