# Backend — Architect AI

Backend em Spring Boot 3 (Java 21) que orquestra o pipeline de análise técnica com agentes IA.

## Requisitos

- Java 21+
- Maven 3.8+
- Git
- (Opcional) Docker e Docker Compose

## Como rodar localmente

```powershell
cd backend
mvn -DskipTests spring-boot:run
```

A aplicação inicia em `http://localhost:8080`.

## Arquitetura — Fluxo de Pipeline

1. **Cliente cria projeto** via `POST /api/v1/projects`
   - Armazena URL do repo e branch padrão

2. **Cliente inicia análise** via `POST /api/v1/projects/{projectId}/analyses`
   - Enfileira job na `BlockingQueue<Analysis>`
   - Retorna status `PENDING`

3. **AnalysisJobProcessor (scheduled)** monitora fila a cada 5 segundos
   - Desfileira análise
   - Muda status para `CLONING`
   - Executa `GitService.cloneRepositoryForAnalysis()`
   - Muda status para `ANALYZING`
   - (TODO) Executa pipeline de análise estática (SpotBugs, PMD, etc.)
   - Muda status para `COMPLETED` ou `FAILED`
   - Persiste relatório (placeholder: S3)

4. **Cliente consulta status** via `GET /api/v1/analyses/{id}`
   - Retorna JSON com status atual, timestamps, resultados

## Endpoints da API

### Projetos

**POST /api/v1/projects** — Criar projeto
```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{"repoUrl":"https://github.com/spring-projects/spring-petclinic.git","defaultBranch":"main"}'
```

Response:
```json
{
  "id": "project-uuid",
  "repoUrl": "https://github.com/spring-projects/spring-petclinic.git",
  "defaultBranch": "main",
  "createdAt": "2026-08-04T10:00:00"
}
```

**GET /api/v1/projects/{id}** — Obter projeto

### Análises

**POST /api/v1/projects/{projectId}/analyses** — Criar análise
```bash
curl -X POST http://localhost:8080/api/v1/projects/{projectId}/analyses \
  -H "Content-Type: application/json" \
  -d '{"type":"CODE_REVIEW"}'
```

Response:
```json
{
  "id": "analysis-uuid",
  "projectId": "project-uuid",
  "type": "CODE_REVIEW",
  "status": "PENDING",
  "createdAt": "2026-08-04T10:00:00"
}
```

Tipos suportados: `CODE_REVIEW`, `ARCHITECTURE`, `PERFORMANCE`, `SECURITY`, `CLOUD`, `DATABASE`, `DUE_DILIGENCE`.

**GET /api/v1/analyses/{id}** — Obter status de análise
```bash
curl http://localhost:8080/api/v1/analyses/{id}
```

**GET /api/v1/projects/{projectId}/analyses** — Listar análises de um projeto
```bash
curl http://localhost:8080/api/v1/projects/{projectId}/analyses
```

**GET /api/v1/analyses/{id}/report** — Download do relatório (quando completed)
- Status `COMPLETED`: retorna URL do relatório
- Status `FAILED`: retorna mensagem de erro
- Status em processamento: retorna 202 Accepted

**GET /api/v1/queue/size** — Tamanho atual da fila (monitoramento)
```bash
curl http://localhost:8080/api/v1/queue/size
```

### Webhooks

**POST /api/v1/webhooks/github** — Webhook GitHub com validação HMAC-SHA256

Configure no GitHub repositório:
- URL: `https://seu-dominio.com/api/v1/webhooks/github`
- Content type: `application/json`
- Secret: valor configurado em `application.yml` ou env var `GITHUB_WEBHOOK_SECRET`
- Events: `Push events`

Payload esperado (estrutura padrão GitHub push event):
```json
{
  "ref": "refs/heads/main",
  "repository": {
    "clone_url": "https://github.com/user/repo.git"
  }
}
```

Validação de segurança:
- Header obrigatório: `X-Hub-Signature-256` (formato `sha256=hexdigest`)
- Verificação HMAC-SHA256 do payload
- Falha se secret não configurado (modo produção)

Teste local com script PowerShell:
```powershell
$secret = "seu-secret-super-secreto-aqui-minimo-32-chars"
$payload = @{
    ref = "refs/heads/main"
    repository = @{
        clone_url = "https://github.com/spring-projects/spring-petclinic.git"
    }
} | ConvertTo-Json

$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
$signature = "sha256=" + ($hmac.ComputeHash($bodyBytes) | ForEach-Object { "{0:x2}" -f $_ } | Join-String)

curl -X POST http://localhost:8080/api/v1/webhooks/github `
  -H "Content-Type: application/json" `
  -H "X-Hub-Signature-256: $signature" `
  -d $payload
```

## Configuração

`src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: architect-ai-backend
  profiles:
    active: dev

github:
  webhook:
    secret: ${GITHUB_WEBHOOK_SECRET:dev-secret-minimo-32-caracteres}
```

Variáveis de ambiente:
- `GITHUB_WEBHOOK_SECRET` — Secret do webhook GitHub (obrigatório em produção)

## Armazenamento (MVP)

- **Projetos**: `ConcurrentHashMap` em memória
- **Análises**: `ConcurrentHashMap` em memória
- **Fila de jobs**: `BlockingQueue` em memória

Em produção:
- Substitua por PostgreSQL + Spring Data JPA
- Mude fila para Kafka/RabbitMQ
- Implemente persistência com Spring Batch

## Status de Análise

- `PENDING` — Na fila, aguardando processamento
- `CLONING` — Repositório sendo clonado
- `ANALYZING` — Pipeline de análise estática em execução
- `COMPLETED` — Análise concluída com sucesso
- `FAILED` — Erro durante processamento

## Próximos Passos

1. Implementar pipeline de análise estática (SpotBugs, PMD, Checkstyle, Semgrep)
2. Integrar AI Orchestrator (Spring AI) e agentes
3. Implementar Report Generator (HTML -> PDF)
4. Adicionar persistência em banco de dados (PostgreSQL)
5. Implementar Storage de artefatos (S3)
6. Adicionar autenticação OAuth2
7. Containerizar com Docker e Kubernetes
