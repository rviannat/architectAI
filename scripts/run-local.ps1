#!/usr/bin/env pwsh
# Script para subir a infra mínima local no Windows PowerShell
Set-StrictMode -Version Latest

Push-Location -Path "$(Split-Path -Path $MyInvocation.MyCommand.Definition -Parent)" | Out-Null
# Move para a raiz do repositório
Pop-Location | Out-Null

Write-Host "Subindo infraestrutura local (docker compose)..." -ForegroundColor Cyan
cd ..\infra
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Falha ao executar 'docker compose up -d'. Verifique se o Docker está instalado e em execução."
    exit 1
}

Write-Host "Infra iniciada. Verifique containers com 'docker ps'" -ForegroundColor Green
Write-Host "Abra 'docs/PRD-ArchitectAI.md' para revisar o PRD e próximos passos." -ForegroundColor Yellow

