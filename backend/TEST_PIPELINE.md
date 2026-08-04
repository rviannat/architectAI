# Teste do Pipeline End-to-End — Architect AI Backend

Este documento descreve como testar o pipeline de análise completo em desenvolvimento local.

## Pré-requisitos

- Backend rodando: `mvn -DskipTests spring-boot:run`
- PowerShell ou terminal com `curl`
- GitHub CLI (opcional, para gerar tokens de teste)

## Teste 1: Criar Projeto

```powershell
$projectBody = @{
    repoUrl = "https://github.com/spring-projects/spring-petclinic.git"
    defaultBranch = "main"
} | ConvertTo-Json

$project = curl -X POST http://localhost:8080/api/v1/projects `
  -H "Content-Type: application/json" `
  -d $projectBody | ConvertFrom-Json

Write-Output "Project ID: $($project.id)"
# Salve o ID para próximos testes
$projectId = $project.id
```

## Teste 2: Iniciar Análise

```powershell
$analysisBody = @{
    type = "CODE_REVIEW"
} | ConvertTo-Json

$analysis = curl -X POST http://localhost:8080/api/v1/projects/$projectId/analyses `
  -H "Content-Type: application/json" `
  -d $analysisBody | ConvertFrom-Json

Write-Output "Analysis ID: $($analysis.id)"
Write-Output "Status: $($analysis.status)"
# Salve o ID para monitoramento
$analysisId = $analysis.id
```

## Teste 3: Monitorar Fila

```powershell
# Verificar tamanho da fila (deve diminuir enquanto o job é processado)
curl http://localhost:8080/api/v1/queue/size
```

## Teste 4: Consultar Status de Análise

```powershell
# Executar múltiplas vezes para ver transição de status
# PENDING -> CLONING -> ANALYZING -> COMPLETED (ou FAILED)

$analysis = curl http://localhost:8080/api/v1/analyses/$analysisId | ConvertFrom-Json

Write-Output "Status: $($analysis.status)"
Write-Output "Created At: $($analysis.createdAt)"
Write-Output "Started At: $($analysis.startedAt)"
Write-Output "Finished At: $($analysis.finishedAt)"
Write-Output "Repository Path: $($analysis.repositoryPath)"
Write-Output "Error: $($analysis.errorMessage)"
Write-Output "Report URL: $($analysis.reportUrl)"
Write-Output "Findings Count: $($analysis.findingsCount)"
```

## Teste 5: Listar Análises de um Projeto

```powershell
$analyses = curl http://localhost:8080/api/v1/projects/$projectId/analyses | ConvertFrom-Json

Write-Output "Total analyses: $($analyses.Count)"
$analyses | ForEach-Object {
    Write-Output "  - ID: $($_.id), Status: $($_.status), Type: $($_.type)"
}
```

## Teste 6: Webhook GitHub com Validação HMAC

### Configurar Secret

```powershell
# Salve em application.yml ou env var
$env:GITHUB_WEBHOOK_SECRET = "seu-secret-super-secreto-aqui-minimo-32-chars"
```

### Gerar Assinatura e Enviar

```powershell
$secret = "seu-secret-super-secreto-aqui-minimo-32-chars"
$payload = @{
    ref = "refs/heads/main"
    repository = @{
        clone_url = "https://github.com/spring-projects/spring-petclinic.git"
        name = "spring-petclinic"
    }
} | ConvertTo-Json -Compress

# Gerar assinatura HMAC-SHA256
$bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
$digest = $hmac.ComputeHash($bodyBytes)
$signature = "sha256=" + ($digest | ForEach-Object { "{0:x2}" -f $_ } | Join-String)

Write-Output "Payload: $payload"
Write-Output "Signature: $signature"

# Enviar webhook com assinatura válida
$webhook = curl -X POST http://localhost:8080/api/v1/webhooks/github `
  -H "Content-Type: application/json" `
  -H "X-Hub-Signature-256: $signature" `
  -d $payload | ConvertFrom-Json

Write-Output "Webhook Response: $($webhook | ConvertTo-Json)"
Write-Output "New Analysis ID: $($webhook.analysisId)"
Write-Output "New Project ID: $($webhook.projectId)"
```

### Teste com Assinatura Inválida (deve retornar 400)

```powershell
$payload = '{"ref":"refs/heads/main","repository":{"clone_url":"https://github.com/user/repo.git"}}'

# Enviar SEM header de assinatura
$response = curl -X POST http://localhost:8080/api/v1/webhooks/github `
  -H "Content-Type: application/json" `
  -d $payload -w "`nStatus: %{http_code}`n"

# Esperado: Status 400
Write-Output $response
```

## Teste 7: Workflow Completo Simulado

```powershell
# Limpar logs anteriores
Clear-Host

# 1. Criar projeto
Write-Output "1. Criando projeto..."
$project = curl -s -X POST http://localhost:8080/api/v1/projects `
  -H "Content-Type: application/json" `
  -d '{"repoUrl":"https://github.com/spring-projects/spring-petclinic.git","defaultBranch":"main"}' | ConvertFrom-Json
$projectId = $project.id
Write-Output "   Project ID: $projectId"

# 2. Iniciar análise
Write-Output "`n2. Iniciando análise..."
$analysis = curl -s -X POST http://localhost:8080/api/v1/projects/$projectId/analyses `
  -H "Content-Type: application/json" `
  -d '{"type":"CODE_REVIEW"}' | ConvertFrom-Json
$analysisId = $analysis.id
Write-Output "   Analysis ID: $analysisId"
Write-Output "   Status: $($analysis.status)"

# 3. Monitorar progresso
Write-Output "`n3. Monitorando progresso (aguarde ~30 segundos)..."
for ($i = 0; $i -lt 6; $i++) {
    Start-Sleep -Seconds 5
    $status = curl -s http://localhost:8080/api/v1/analyses/$analysisId | ConvertFrom-Json
    Write-Output "   [$($i*5)s] Status: $($status.status)"
    
    if ($status.status -eq "COMPLETED" -or $status.status -eq "FAILED") {
        break
    }
}

# 4. Resultados finais
Write-Output "`n4. Resultados finais:"
$final = curl -s http://localhost:8080/api/v1/analyses/$analysisId | ConvertFrom-Json
Write-Output "   Status: $($final.status)"
Write-Output "   Created: $($final.createdAt)"
Write-Output "   Started: $($final.startedAt)"
Write-Output "   Finished: $($final.finishedAt)"
Write-Output "   Repo Path: $($final.repositoryPath)"
Write-Output "   Findings: $($final.findingsCount)"
Write-Output "   Report: $($final.reportUrl)"
if ($final.errorMessage) {
    Write-Output "   Error: $($final.errorMessage)"
}
```

## Logs Esperados (no console do backend)

```
INFO ... AnalysisJobProcessor : Processing analysis: {analysisId} for project: {projectId}
INFO ... AnalysisJobProcessor : Cloning repository for analysis: {analysisId}
INFO ... AnalysisJobProcessor : Running static analysis for: {analysisId}
INFO ... AnalysisJobProcessor : Analysis completed successfully: {analysisId}
```

## Troubleshooting

**Problema**: Análise fica em status `PENDING` por muito tempo
- Verificar se `AnalysisJobProcessor` está ativo (logs)
- Verificar fila com endpoint `/api/v1/queue/size`
- Aumentar intervalo de polling (em `AnalysisJobProcessor.processAnalysisQueue`)

**Problema**: Clonagem falha
- Verificar se repositório é acessível (URL correta)
- Verificar permissões de escrita em `./workspace/`
- Para repos privadas, passar token via header `X-Git-Token`

**Problema**: Webhook retorna 400
- Verificar se secret está configurado em `application.yml`
- Gerar assinatura com mesmo secret
- Verificar payload em JSON válido

**Problema**: Port 8080 já em uso
```powershell
# Mudar porta em application.yml
# server.port: 8081
```

## Próximos Passos

Após validar que o pipeline completo funciona:
1. Implementar Static Analysis Engine (SpotBugs, PMD, etc.)
2. Integrar AI Orchestrator para processamento de findings
3. Implementar Report Generator (PDF)
4. Adicionar testes automatizados (JUnit 5)

