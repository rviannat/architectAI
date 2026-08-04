# Próximos Passos — Roadmap Detalhado para B3, C, D, E

## Resumo: O que já foi feito

✅ **Fase 1**: PRD completo
✅ **Fase 2**: Backend skeleton (Spring Boot 3, Java 21)
✅ **Fase 3**: Git Connector (JGit clone) + Webhook GitHub com HMAC
✅ **Fase 4 (B2)**: Pipeline completo com fila de jobs + orquestração

**Próximos: B3 → C → D → E**

---

## B3 — Static Analysis Engine (Semana 1)

### Objetivo
Implementar execução de ferramentas de análise estática e consolidar findings estruturados.

### Escopo

#### B3.1 — SpotBugs Integration (2 dias)
- [x] Instalação e configuração do SpotBugs CLI
- [x] Criar `SpotBugsService` com wrapper
- [x] Parser de output XML → JSON estruturado
- [x] Integrar no pipeline (após clone, antes de finish)

Exemplo de finding estruturado:
```json
{
  "tool": "SpotBugs",
  "id": "BUG-001",
  "type": "NullPointerException",
  "severity": "HIGH",
  "file": "src/main/java/com/x/Service.java",
  "line": 128,
  "message": "Potential null pointer dereference",
  "evidence": "obj.method()",
  "tags": ["reliability", "null-safety"]
}
```

#### B3.2 — PMD Integration (2 dias)
- [x] Instalação PMD CLI
- [x] Criar `PMDService` com wrapper
- [x] Parser de output XML → JSON
- [x] Integrar no pipeline

Focusar em:
- Best Practices
- Code Style
- Design Flaws
- Documentation

#### B3.3 — Checkstyle Integration (1 dia)
- [x] Instalação Checkstyle
- [x] Criar `CheckstyleService`
- [x] Parser JSON output
- [x] Integrar no pipeline

#### B3.4 — Semgrep Integration (2 dias)
- [x] Instalação Semgrep CLI (opcional: usar API)
- [x] Criar `SemgrepService`
- [x] Parser JSON output
- [x] Custom rules para OWASP Top 10, security patterns

#### B3.5 — Consolidação de Findings (1 dia)
- [x] Criar `FindingsConsolidationService`
- [x] Deduplicate findings por file:line
- [x] Priorizar por severity
- [x] Agrupar por categoria/tool
- [x] Retornar JSON estruturado

### Arquivos a Criar
```
backend/src/main/java/com/architectai/backend/
├── service/
│   ├── SpotBugsService.java (novo)
│   ├── PMDService.java (novo)
│   ├── CheckstyleService.java (novo)
│   ├── SemgrepService.java (novo)
│   ├── FindingsConsolidationService.java (novo)
│   ├── StaticAnalysisExecutor.java (novo) ← orquestra os 4 acima
│   └── AnalysisJobProcessor.java (modificado: chamar StaticAnalysisExecutor)
└── model/
    └── Finding.java (novo) ← estrutura padronizada
```

### Integração no Pipeline
```
AnalysisJobProcessor.processAnalysis()
│
├─ CLONING: GitService.clone()
│
├─ ANALYZING:
│  ├─ SpotBugsService.analyze()
│  ├─ PMDService.analyze()
│  ├─ CheckstyleService.analyze()
│  ├─ SemgrepService.analyze()
│  └─ FindingsConsolidationService.consolidate()
│     → List<Finding> (estruturado, deduped, priorizado)
│
└─ COMPLETED: completeAnalysis(findingsCount, reportUrl)
```

### Testes (B3)
- Clonar repo de teste (ex: spring-petclinic)
- Rodar cada ferramenta isoladamente
- Validar output JSON
- Validar consolidação (dedupe + priorização)
- E-2-E test: POST /analyses → completa com findings

### Configuração (application.yml)
```yaml
static-analysis:
  tools:
    spotbugs:
      enabled: true
      path: /usr/bin/spotbugs # ou auto-detect
      timeout-seconds: 60
    pmd:
      enabled: true
      path: /usr/bin/pmd
      timeout-seconds: 60
    checkstyle:
      enabled: true
      path: /usr/bin/checkstyle
      timeout-seconds: 30
    semgrep:
      enabled: true
      path: semgrep # via pip install
      timeout-seconds: 120
```

### Dependências a Adicionar
Nenhuma — usar CLI nativo (ProcessBuilder)

---

## C — Containerização (3 dias)

### Objetivo
Empacotar backend, criar docker-compose com PostgreSQL, Redis, MinIO.

### Escopo

#### C.1 — Dockerfile (backend)
```dockerfile
FROM eclipse-temurin:21-jdk-alpine

# Install static analysis tools
RUN apk add --no-cache bash curl unzip
RUN mkdir -p /app/tools

# SpotBugs
RUN curl -o /tmp/spotbugs.zip https://repo.maven.apache.org/maven2/com/google/code/findbugs/spotbugs/4.8.1/spotbugs-4.8.1.zip && \
    unzip /tmp/spotbugs.zip -d /app/tools

# PMD, Checkstyle, Semgrep (similar)

WORKDIR /app
COPY target/backend-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

#### C.2 — docker-compose.yml (atualizar)
```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/architectai
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      GITHUB_WEBHOOK_SECRET: ${GITHUB_WEBHOOK_SECRET}
    depends_on:
      - postgres
      - redis

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: architectai
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  minio:
    image: minio/minio:latest
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/minio_data
    command: server /minio_data --console-address ":9001"

volumes:
  postgres-data:
  minio-data:
```

#### C.3 — GitHub Actions CI/CD
- Maven build
- Unit tests
- Docker build + push (Docker Hub / ECR)
- Scan segurança (Trivy, OWASP)

#### C.4 — Helm Charts (opcional, para Kubernetes)
- Chart para backend
- Chart para PostgreSQL
- Chart para Redis
- Values configuráveis

### Arquivos a Criar
```
backend/
├── Dockerfile (novo)
├── .dockerignore (novo)
└── docker/
    ├── entrypoint.sh (novo)
    └── tools-install.sh (novo)

infra/
└── docker-compose.yml (atualizar)

.github/workflows/
├── build.yml (novo) — Maven build
├── docker.yml (novo) — Build & push Docker
└── security.yml (novo) — Trivy scan

helm/
├── architect-ai/
│   ├── Chart.yaml (novo)
│   ├── values.yaml (novo)
│   ├── templates/
│   │   ├── deployment.yaml (novo)
│   │   ├── service.yaml (novo)
│   │   └── configmap.yaml (novo)
│   └── README.md (novo)
```

### Deploy Local (após C)
```bash
docker compose up -d
# Validar: http://localhost:8080/actuator/health
```

### Deploy Produção (Kubernetes)
```bash
helm install architect-ai ./helm/architect-ai \
  --set image.tag=v1.0.0 \
  --set postgresPassword=secure-pwd
```

---

## D — Persistência (5 dias)

### Objetivo
Migrar de in-memory para PostgreSQL com Spring Data JPA.

### Escopo

#### D.1 — Entities JPA
- [x] Converter `Analysis` → `@Entity`
- [x] Converter `Project` → `@Entity`
- [x] Adicionar `Finding` → `@Entity`
- [x] Adicionar `AgentRun` → `@Entity`
- [x] Adicionar `Report` → `@Entity`
- [x] Relationships: Project → Analysis (1:N), Analysis → Finding (1:N)

Exemplo:
```java
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String repoUrl;
    private String defaultBranch;
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<Analysis> analyses;
}
```

#### D.2 — Repositories (Spring Data)
- [x] `ProjectRepository extends JpaRepository<Project, String>`
- [x] `AnalysisRepository extends JpaRepository<Analysis, String>`
- [x] `FindingRepository extends JpaRepository<Finding, String>`
- [x] Queries customizadas (findByProjectId, etc.)

#### D.3 — Migrations (Flyway)
- [x] V1__initial_schema.sql (projects, analyses, findings)
- [x] V2__add_agent_runs.sql
- [x] V3__add_reports.sql

#### D.4 — Serviços Atualizados
- [x] `ProjectService` → usar `ProjectRepository`
- [x] `AnalysisService` → usar `AnalysisRepository`
- [x] `FindingsService` → usar `FindingRepository`

#### D.5 — Testes de Integridade
- [x] Tests JPA (TestContainers com PostgreSQL)
- [x] Validar relacionamentos
- [x] Validar cascades
- [x] Validar queries customizadas

### Dependências a Adicionar
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- Tests -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### Configuração (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/architectai
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    locations: classpath:db/migration
```

### Arquivos a Criar
```
backend/src/main/java/com/architectai/backend/
├── entity/ (novo diretório)
│   ├── ProjectEntity.java
│   ├── AnalysisEntity.java
│   ├── FindingEntity.java
│   ├── AgentRunEntity.java
│   └── ReportEntity.java
├── repository/ (novo diretório)
│   ├── ProjectRepository.java
│   ├── AnalysisRepository.java
│   └── FindingRepository.java
└── service/ (modificado)
    ├── ProjectService.java (agora usa repository)
    ├── AnalysisService.java (agora usa repository)
    └── FindingsService.java (novo)

backend/src/main/resources/db/migration/
├── V1__initial_schema.sql (novo)
├── V2__add_agent_runs.sql (novo)
└── V3__add_reports.sql (novo)

backend/src/test/java/com/architectai/backend/
└── repository/ (novo diretório)
    ├── ProjectRepositoryTest.java
    └── AnalysisRepositoryTest.java
```

### Testes (D)
- Testes JPA com TestContainers
- Validar migrations
- Validar relacionamentos (lazy loading, eager loading)
- Performance (índices em PostgreSQL)

---

## E — Frontend (2 semanas)

### Objetivo
Criar dashboard para gerenciar análises.

### Escopo

#### E.1 — Setup Next.js (1 dia)
- [x] `create-next-app architect-ai-frontend`
- [x] Configurar Tailwind CSS
- [x] Configurar shadcn/ui
- [x] ESLint + Prettier

#### E.2 — Pages & Routing (3 dias)
```
pages/
├── index.tsx .................. Landing / Dashboard
├── projects/
│   ├── index.tsx ............. Lista de projetos
│   ├── [id].tsx .............. Detalhes do projeto
│   └── new.tsx ............... Criar novo projeto
├── analyses/
│   ├── [id].tsx .............. Detalhes da análise
│   └── [id]/report.tsx ....... Visualizar relatório
└── api/
    └── [proxy].ts ............ API proxy (opcional)
```

#### E.3 — Components (5 dias)
- [x] `ProjectForm` — criar/editar projeto
- [x] `ProjectList` — tabela com paginação
- [x] `AnalysisCard` — status + ícones
- [x] `AnalysisTimeline` — PENDING → CLONING → ANALYZING → COMPLETED
- [x] `FindingsTable` — lista de findings com filtro/sort
- [x] `ReportViewer` — embed PDF (pdfjs ou iframe)
- [x] `Queue` — visualizar tamanho da fila em tempo real

#### E.4 — Integração com Backend (3 dias)
- [x] `useProjects` hook (fetch, CRUD)
- [x] `useAnalyses` hook (fetch by project, real-time polling)
- [x] `useFindingsn` hook (fetch, filtrar)
- [x] `useWebSocket` (opcional: SSE para atualizações real-time)

#### E.5 — Autenticação (2 dias)
- [x] NextAuth.js com GitHub OAuth
- [x] Protected routes
- [x] User context

#### E.6 — Testes (2 dias)
- [x] Jest + React Testing Library
- [x] Unit tests de componentes
- [x] Integration tests (mock API)

### Tech Stack
- Next.js 14 (App Router)
- React 18
- TypeScript
- Tailwind CSS
- shadcn/ui
- TanStack Query (React Query)
- Axios (HTTP)
- Jest + React Testing Library

### Arquivo estrutura
```
frontend/
├── app/
│   ├── layout.tsx ........... Root layout
│   ├── page.tsx ............ Dashboard
│   ├── projects/
│   │   ├── page.tsx ........ Lista
│   │   ├── new/page.tsx .... Criar
│   │   └── [id]/page.tsx ... Detalhes
│   └── analyses/
│       └── [id]/page.tsx ... Detalhes análise
├── components/
│   ├── ProjectForm.tsx
│   ├── ProjectList.tsx
│   ├── AnalysisCard.tsx
│   ├── FindingsTable.tsx
│   └── ... (outros)
├── hooks/
│   ├── useProjects.ts
│   ├── useAnalyses.ts
│   └── useFindingsn.ts
├── lib/
│   ├── api.ts .............. Cliente HTTP
│   ├── auth.ts ............ NextAuth config
│   └── types.ts ........... Interfaces TypeScript
├── styles/
│   └── globals.css ........ Tailwind
└── ... (testes, config)
```

### Features MVP (E)
- [x] Dashboard com últimas análises
- [x] CRUD de projetos
- [x] Visualizar status de análise (real-time)
- [x] Download de relatórios
- [x] Filtro/busca de findings
- [x] Webhook GitHub configuração (manual)

### Features Futuros (E+)
- [ ] Gráficos de tendência (dívida técnica over time)
- [ ] Comparação entre análises (before/after)
- [ ] Notificações (email, Slack)
- [ ] Multi-tenancy (orgs/teams)
- [ ] CI/CD integration (GitHub, GitLab, Bitbucket)

---

## Roadmap Temporal Recomendado

```
Semana 1 (B3 — Static Analysis)
├─ Mon-Tue: SpotBugs + PMD
├─ Wed-Thu: Checkstyle + Semgrep
└─ Fri: Consolidação + Testes

Semana 2 (C — Containerização)
├─ Mon: Dockerfile + docker-compose
├─ Tue-Wed: CI/CD (GitHub Actions)
├─ Thu-Fri: Helm charts (opcional)

Semana 3 (D — Persistência)
├─ Mon-Tue: Entities + Repositories
├─ Wed: Migrations (Flyway)
├─ Thu-Fri: Testes (TestContainers)

Semana 4-5 (E — Frontend)
├─ Mon: Next.js setup
├─ Tue-Wed: Pages e Components
├─ Thu-Fri: Integração API + Autenticação
├─ Semana 5: Testes + Deploy

TOTAL: ~5 semanas para MVP completo
```

---

## Dependências & Requisitos

### B3
- SpotBugs CLI (via Dockerfile ou manual install)
- PMD CLI
- Checkstyle CLI
- Semgrep CLI (pip install)

### C
- Docker Desktop
- docker-compose

### D
- PostgreSQL 15+
- Testcontainers

### E
- Node.js 18+
- npm/yarn
- GitHub OAuth app (para NextAuth)

---

## Métricas de Sucesso

| Fase | Métrica | Target |
|------|---------|--------|
| B3 | Findings count | >= 50 por projeto |
| B3 | False positive rate | < 10% |
| C | Docker build time | < 2 min |
| C | Image size | < 500MB |
| D | Query latency | < 100ms p95 |
| D | Test coverage | > 70% |
| E | Core metrics | LCP < 1.5s, FID < 100ms |
| E | Test coverage | > 60% |

---

## Conclusão

Implementando **B3 → C → D → E** em sequência, teremos um **MVP completo e pronto para SaaS** em ~5 semanas.

Próximo passo: **Iniciar B3 (Static Analysis Engine)**.

Quer que eu comece com B3 agora?

