##############################################################
# Teste End-to-End -- 3 Repositorios Reais
# ArchitectAI -- Camadas 2 + 3
##############################################################

$BASE = "http://localhost:8080/api/v1"
$REPOS = @(
    @{ name = "spring-petclinic";     url = "https://github.com/spring-projects/spring-petclinic.git"; branch = "main" },
    @{ name = "java-design-patterns"; url = "https://github.com/iluwatar/java-design-patterns.git";    branch = "master" },
    @{ name = "baeldung-tutorials";   url = "https://github.com/eugenp/tutorials.git";                 branch = "master" }
)

function Invoke-Api {
    param($method, $url, $body = $null)
    $params = @{ Uri = $url; Method = $method; ContentType = "application/json"; ErrorAction = "Stop" }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Compress) }
    return Invoke-RestMethod @params
}

function Write-Sep  { Write-Host ("`n" + ("=" * 65)) -ForegroundColor Cyan }
function Write-Ok   { param($m); Write-Host "  [OK]  $m" -ForegroundColor Green }
function Write-Warn { param($m); Write-Host "  [!!]  $m" -ForegroundColor Yellow }
function Write-Err  { param($m); Write-Host "  [ERR] $m" -ForegroundColor Red }
function Write-Info { param($m); Write-Host "  [--]  $m" -ForegroundColor White }

##############################################################
# Verifica backend
##############################################################
Write-Sep
Write-Host " VERIFICANDO BACKEND" -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod "http://localhost:8080/actuator/health" -ErrorAction Stop
    Write-Ok "Backend UP -- status: $($health.status)"
} catch {
    Write-Err "Backend nao responde. Inicie com: mvn -DskipTests spring-boot:run"
    exit 1
}

##############################################################
# Loop pelos repositorios
##############################################################
$results = @()

foreach ($repo in $REPOS) {
    Write-Sep
    Write-Host " REPOSITORIO: $($repo.name)" -ForegroundColor Cyan
    Write-Info "URL: $($repo.url)"

    # 1. Criar projeto
    Write-Host "  [1/4] Criando projeto..." -NoNewline
    $projectId = $null
    try {
        $proj = Invoke-Api "POST" "$BASE/projects" @{ repoUrl = $repo.url; defaultBranch = $repo.branch }
        $projectId = $proj.data.id
        Write-Ok "projectId = $projectId"
    } catch {
        Write-Err "Falha ao criar projeto: $_"
        $results += [PSCustomObject]@{
            Repo = $repo.name; ProjectId = "N/A"; AnalysisId = "N/A"
            Status = "CREATE_FAIL"; Findings = 0; Report = ""; Error = "$_"
        }
        continue
    }

    # 2. Iniciar analise
    Write-Host "  [2/4] Iniciando analise..." -NoNewline
    $analysisId = $null
    try {
        $analysis = Invoke-Api "POST" "$BASE/projects/$projectId/analyses" @{ type = "CODE_REVIEW" }
        $analysisId = $analysis.data.id
        Write-Ok "analysisId = $analysisId  |  status = $($analysis.data.status)"
    } catch {
        Write-Err "Falha ao iniciar analise: $_"
        $results += [PSCustomObject]@{
            Repo = $repo.name; ProjectId = $projectId; AnalysisId = "N/A"
            Status = "ANALYSIS_FAIL"; Findings = 0; Report = ""; Error = "$_"
        }
        continue
    }

    # 3. Monitorar (max 180 segundos)
    Write-Host "  [3/4] Monitorando progresso (max 180s)..."
    $elapsed = 0
    $finalStatus = $null
    $finalData = $null

    for ($i = 0; $i -lt 36; $i++) {
        Start-Sleep -Seconds 5
        $elapsed += 5
        try {
            $cur = Invoke-Api "GET" "$BASE/analyses/$analysisId"
            $st = $cur.data.status
            Write-Info "    [${elapsed}s] $st"
            if ($st -eq "COMPLETED" -or $st -eq "FAILED") {
                $finalStatus = $st
                $finalData = $cur.data
                break
            }
        } catch {
            Write-Warn "    Erro ao consultar: $_"
        }
    }

    if (-not $finalStatus) {
        $finalStatus = "TIMEOUT"
        try {
            $finalData = (Invoke-Api "GET" "$BASE/analyses/$analysisId").data
        } catch {}
    }

    # 4. Resultado
    Write-Host "  [4/4] Resultado final:"
    $cor = "Yellow"
    if ($finalStatus -eq "COMPLETED") { $cor = "Green" }
    if ($finalStatus -eq "FAILED")    { $cor = "Red" }
    Write-Host "    Status   : $finalStatus" -ForegroundColor $cor

    $findings   = 0
    $reportPath = ""
    $errMsg     = ""
    if ($finalData) {
        $findings   = $finalData.findingsCount
        $reportPath = $finalData.reportUrl
        $errMsg     = $finalData.errorMessage
        Write-Info "    Findings : $findings"
        Write-Info "    Report   : $reportPath"
        if ($errMsg) { Write-Warn "    Error    : $errMsg" }

        $byTool = $finalData.staticAnalysisFindingsByTool
        if ($byTool) {
            Write-Info "    Static findings por ferramenta:"
            $byTool.PSObject.Properties | ForEach-Object {
                Write-Info "      $($_.Name) : $($_.Value)"
            }
        }
    }

    $results += [PSCustomObject]@{
        Repo      = $repo.name
        ProjectId = $projectId
        AnalysisId= $analysisId
        Status    = $finalStatus
        Findings  = $findings
        Report    = $reportPath
        Error     = $errMsg
    }
}

##############################################################
# Sumario final
##############################################################
Write-Sep
Write-Host " SUMARIO DO TESTE E2E" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$ok    = ($results | Where-Object { $_.Status -eq "COMPLETED" }).Count
$total = $results.Count

if ($ok -eq $total) {
    Write-Ok "TODOS OS REPOSITORIOS PROCESSADOS COM SUCESSO ($ok/$total)"
} else {
    Write-Warn "$ok de $total concluidos -- verifique os itens acima"
}
Write-Sep
