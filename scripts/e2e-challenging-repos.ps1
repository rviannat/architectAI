param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [int]$PollIntervalSeconds = 10,
    [int]$MaxSecondsPerRepo = 900
)

$ErrorActionPreference = "Stop"

$repos = @(
    @{ name = "spring-boot";      url = "https://github.com/spring-projects/spring-boot.git";      branch = "main" },
    @{ name = "spring-framework"; url = "https://github.com/spring-projects/spring-framework.git"; branch = "main" },
    @{ name = "baeldung-tutorials"; url = "https://github.com/eugenp/tutorials.git"; branch = "master" }
)

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

$results = @()

foreach ($repo in $repos) {
    Write-Host "\n=== $($repo.name) ===" -ForegroundColor Cyan

    $project = Invoke-Api -Method "POST" -Url "$BaseUrl/projects" -Body @{ repoUrl = $repo.url; defaultBranch = $repo.branch }
    $projectId = $project.data.id
    Write-Host "projectId: $projectId"

    $analysis = Invoke-Api -Method "POST" -Url "$BaseUrl/projects/$projectId/analyses" -Body @{ type = "CODE_REVIEW" }
    $analysisId = $analysis.data.id
    Write-Host "analysisId: $analysisId"

    $elapsed = 0
    $status = $analysis.data.status

    while ($elapsed -lt $MaxSecondsPerRepo) {
        Start-Sleep -Seconds $PollIntervalSeconds
        $elapsed += $PollIntervalSeconds

        $current = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId"
        $status = $current.data.status
        Write-Host "[$elapsed s] status=$status"

        if ($status -eq "COMPLETED" -or $status -eq "FAILED") {
            break
        }
    }

    $final = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId"
    $reports = $null
    try {
        $reports = Invoke-Api -Method "GET" -Url "$BaseUrl/analyses/$analysisId/reports"
    } catch {
        # reports endpoint can fail for incomplete analyses
    }

    $results += [PSCustomObject]@{
        repo = $repo.name
        repoUrl = $repo.url
        projectId = $projectId
        analysisId = $analysisId
        status = $final.data.status
        findingsCount = $final.data.findingsCount
        reportUrl = $final.data.reportUrl
        downloadUrl = "$BaseUrl/analyses/$analysisId/report/download"
        technicalReportUrl = if ($reports) { $reports.data.technicalReportUrl } else { $null }
        commercialReportUrl = if ($reports) { $reports.data.commercialReportUrl } else { $null }
        unifiedReportUrl = if ($reports) { $reports.data.unifiedReportUrl } else { $null }
        staticFindingsByTool = $final.data.staticAnalysisFindingsByTool
        errorMessage = $final.data.errorMessage
    }
}

$resultPath = "C:\Users\User\Documents\repositorio\architectAI\ArchitectAI\scripts\e2e-challenging-results.json"
$results | ConvertTo-Json -Depth 8 | Set-Content -Path $resultPath -Encoding UTF8

Write-Host "\nSaved results to: $resultPath" -ForegroundColor Green
$results | ConvertTo-Json -Depth 8

