# Camada C — Containerização e Setup

Este documento explica como subir a infraestrutura local, rodar o backend em Docker e entender as variáveis de ambiente usadas pela aplicação.

## Objetivo

A Camada C adiciona:

- `backend/Dockerfile` para empacotar o Spring Boot em uma imagem
- `infra/docker-compose.yml` com:
  - PostgreSQL
  - Redis
  - MinIO
  - Backend
- `.github/workflows/ci.yml` para validação contínua
- Configuração de runtime via variáveis de ambiente

## Pré-requisitos

- Docker Desktop instalado e em execução
- Docker Compose v2
- Java 21 e Maven 3.8+ caso queira executar fora do container

## Estrutura principal

```text
backend/Dockerfile
infra/docker-compose.yml
.github/workflows/ci.yml
docs/CAMADA_C_SETUP.md
```

## Como subir a stack local

Na raiz do repositório:

```powershell
cd infra
docker compose up --build
```

Isso sobe:

- `postgres` em `localhost:5432`
- `redis` em `localhost:6379`
- `minio` em `localhost:9000` e console em `localhost:9001`
- `backend` em `localhost:8080`

## Acessos

### Backend

- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/api-docs`

### MinIO

- Console: `http://localhost:9001`
- Usuário: `minioadmin`
- Senha: `minioadmin`

### PostgreSQL

- Host: `localhost`
- Porta: `5432`
- Banco: `architectai`
- Usuário: `architect`
- Senha: `architect`

## Variáveis de ambiente

As principais variáveis usadas pelo backend são:

```powershell
$env:GITHUB_WEBHOOK_SECRET = "dev-secret-change-in-production"
$env:ARCHITECTAI_RUNTIME_WORKSPACE_DIR = "./workspace"
$env:ARCHITECTAI_RUNTIME_REPORTS_DIR = "./.architectai/reports"
```

Quando executado dentro do Docker, os valores já são definidos no `docker-compose.yml`.

## Como rodar o backend local sem Docker

Se você quiser executar só o backend:

```powershell
cd backend
mvn spring-boot:run
```

## Como gerar a imagem manualmente

```powershell
cd backend
docker build -t architectai-backend:latest .
```

## Como validar a imagem

```powershell
docker run --rm -p 8080:8080 `
  -e GITHUB_WEBHOOK_SECRET=dev-secret-change-in-production `
  -e ARCHITECTAI_RUNTIME_WORKSPACE_DIR=/data/workspace `
  -e ARCHITECTAI_RUNTIME_REPORTS_DIR=/data/reports `
  architectai-backend:latest
```

## Pipeline de CI

O workflow em `.github/workflows/ci.yml` faz:

1. Checkout do código
2. Setup do Java 21
3. Build e testes com Maven
4. Empacotamento do backend

## Observações importantes

- O backend ainda usa storage local para PDFs e workspace, mas agora os caminhos são configuráveis.
- A stack já está pronta para evoluir para persistência real de relatórios em banco ou object storage.
- Os volumes do Docker garantem que workspace e relatórios não se percam entre reinícios.

## Próximo passo recomendado

Depois da Camada C, o próximo passo natural é a **persistência real** dos projetos, análises e relatórios no banco de dados.

