param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [string]$RepoUrl = "https://github.com/rviannat/poverty-ai-platform-backend",
    [string]$DefaultBranch = "main",
    [string]$OutputDir = "C:\Users\User\Documents\repositorio\architectAI\ArchitectAI\artifacts\pdfs",
    [int]$PollIntervalSeconds = 10,
    [int]$MaxSeconds = 900
)

$ErrorActionPreference = "Stop"

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body
    )

    $params = @{
        Uri = $Url
        Method = $Method
        ContentType = "application/json"
        ErrorAction = "Stop"
    }

    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress)
    }

    return Invoke-RestMethod @params
}

$health = Invoke-RestMethod "http://localhost:8080/actuator/health"
if ($health.status -ne "UP") {
    throw "Backend not healthy. status=$($health.status)"
}

Write-Host "Backend OK" -ForegroundColor Green
Write-Host "Repo: $RepoUrl" -ForegroundColor Cyan

$projects = Invoke-Api -Method "GET" -Url "$BaseUrl/projects"
$existingProject = $projects.data | Where-Object { $_.repoUrl -eq $RepoUrl } | Select-Object -First 1

if ($existingProject) {
    $projectId = $existingProject.id
    Write-Host "Using existing project: $projectId" -ForegroundColor Yellow
}
else {
    $project = Invoke-Api -Method "POST" -Url "$BaseUrl/projects" -Body @{ repoUrl = $RepoUrl; defaultBranch = $DefaultBranch }
    $projectId = $project.data.id
    Write-Host "Created project: $projectId" -ForegroundColor Green
}

$analysis = Invoke-Api -Method "POST" -Url "$BaseUrl/projects/$projectId/analyses" -Body @{ type = "CODE_REVIEW" }
$analysisId = $analysis.data.id
Write-Host "analysisId: $analysisId"

$elapsed = 0
$status = $analysis.data.status

while ($elapsed -lt $MaxSeconds) {
    Start-Sleep -Seconds $PollIntervalSeconds
    $elapsed += $PollIntervalSeconds

    try {
        $current = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId"
        $status = $current.data.status
        Write-Host "[$elapsed s] status=$status"
        if ($status -eq "COMPLETED" -or $status -eq "FAILED") {
            break
        }
    }
    catch {
        Write-Host "[$elapsed s] status=ERROR" -ForegroundColor Yellow
    }
}

$final = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId"
$reports = $null
try {
    $reports = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId/reports"
}
catch {
    # Ignore when report package is not available.
}

$result = [PSCustomObject]@{
    projectId = $projectId
    analysisId = $analysisId
    status = $final.data.status
    findingsCount = $final.data.findingsCount
    errorMessage = $final.data.errorMessage
    reportUrl = $final.data.reportUrl
    technicalReportUrl = if ($reports) { $reports.data.technicalReportUrl } else { $null }
    commercialReportUrl = if ($reports) { $reports.data.commercialReportUrl } else { $null }
    unifiedReportUrl = if ($reports) { $reports.data.unifiedReportUrl } else { $null }
    manifestUrl = if ($reports) { $reports.data.manifestUrl } else { $null }
    staticAnalysisFindingsByTool = $final.data.staticAnalysisFindingsByTool
}

$resultPath = Join-Path $PSScriptRoot "one-analysis-result.json"
$result | ConvertTo-Json -Depth 10 | Set-Content -Path $resultPath -Encoding UTF8

$savedPdfPath = $null
if ($final.data.status -eq "COMPLETED") {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    $pdfFile = Join-Path $OutputDir ("analysis_" + $analysisId + ".pdf")
    Invoke-WebRequest -Uri "$BaseUrl/analyses/$analysisId/report/download" -OutFile $pdfFile
    $savedPdfPath = $pdfFile
    Write-Host "Saved PDF to: $savedPdfPath" -ForegroundColor Green
}

Write-Host "Saved result to: $resultPath" -ForegroundColor Green
([PSCustomObject]@{
    result = $result
    savedPdfPath = $savedPdfPath
}) | ConvertTo-Json -Depth 10

