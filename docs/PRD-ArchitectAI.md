# PRD — Architect AI

Versão inicial do Product Requirements Document para o projeto Architect AI.

## Resumo executivo
- Nome do produto: Architect AI
- Slogan: AI-powered Software Architecture & Engineering Consulting Platform
- Proposta de valor: entregar auditorias técnicas e recomendações de arquitetura de alta qualidade em horas/dias, combinando análise estática tradicional com agentes especialistas de IA. O cliente compra resultado — não IA.

## Visão
O Architect AI é uma plataforma de consultoria técnica assistida por IA capaz de analisar aplicações corporativas, identificar problemas arquiteturais, vulnerabilidades, gargalos de desempenho e oportunidades de melhoria, produzindo relatórios executivos e técnicos automaticamente.

## Objetivos do produto
- Automatizar ~80% da auditoria técnica para reduzir tempo e custo.
- Permitir que um arquiteto valide e finalize as análises automatizadas.
- Gerar entregáveis padronizados: PDF executivo, relatório técnico e backlog priorizado.

## Público-alvo
- Empresas com times de engenharia médios e grandes (20+ devs)
- Stacks Java/Spring, fintechs, bancos, seguradoras, healthtechs e software houses.

## Catálogo de serviços (MVP)
- Code Review Inteligente — diagnóstico rápido (R$ 2.000)
- Auditoria de Arquitetura — análise profunda (R$ 5.000+)
- Performance — análise JVM/queries/cache (R$ 3.000)
- Auditoria Cloud — custos, segurança e backups
- Due Diligence Técnica — para investidores
- AI Pair Programming — agente acompanhante (assinatura)

## Entregáveis
- PDF profissional (capa, sumário executivo, findings, roadmap, apêndice técnico)
- JSON estruturado com findings (schema padronizado)
- Dashboard simples para download e histórico

## Escopo do MVP (30 dias)
- Conectar a um repositório GitHub e clonar
- Identificar stack automaticamente
- Executar análise estática (SpotBugs, PMD, Checkstyle, Semgrep)
- Orquestrar agentes IA (Java Specialist, Tech Lead inicial)
- Gerar relatório executivo (HTML -> PDF)
- Web UI simples (landing + painel)

## Arquitetura de alto nível
Componentes principais:
- Portal Web (Next.js)
- API (Spring Boot 3, Java 21)
- Git Connector
- Static Analysis Engine (wrappers para SpotBugs/PMD/Checkstyle/Semgrep)
- AI Orchestrator (abstração de providers)
- Agents especializados (workers)
- Report Generator (HTML -> PDF)
- Knowledge Base (Postgres + pgvector)
- Storage (S3/MinIO)
- Queue (Kafka ou Redis Streams)

Fluxo simplificado:
Cliente envia repositório -> Git Connector clona -> Static Analysis -> AI Orchestrator -> Agents -> Tech Lead consolida -> Report Generator -> Entrega ao cliente

## Agentes especializados (inicial)
- Software Architect
- Java Specialist
- Performance Engineer
- Security Engineer
- Cloud Architect
- Database Specialist
- DevOps Engineer
- Tech Lead (orquestrador)

Cada agente deve retornar um JSON com o schema de findings padronizado (id, agent, severity, file, line, title, description, recommendation, estimated_effort_hours, confidence, references).

## Integrações e ferramentas
- SpotBugs, PMD, Checkstyle, Semgrep
- SonarQube (opcional)
- Dependabot / Snyk
- GitHub Apps e GitHub Actions
- OpenAI / Anthropic / Gemini (via AI Orchestrator)

## Stack técnico recomendado
- Backend: Java 21, Spring Boot 3
- IA: Spring AI + providers
- DB: PostgreSQL + pgvector
- Cache: Redis
- Storage: S3 / MinIO
- Messaging: Kafka (ou Redis Streams no MVP)
- Frontend: Next.js + React + Tailwind
- Infra: Docker, Kubernetes, Terraform

## Modelo de dados mínimo (resumo)
- users, organizations, projects, analyses, findings, reports, agent_runs, embeddings

Exemplo de saída JSON padronizada por agente:

```json
{
  "findings": [
    {
      "id": "F-001",
      "agent": "Java Specialist",
      "severity": "HIGH",
      "file": "src/main/java/com/x/Service.java",
      "line": 128,
      "title": "Uso ineficiente de consulta JPA",
      "description": "...",
      "recommendation": "...",
      "estimated_effort_hours": 8,
      "confidence": 0.92,
      "references": ["link"]
    }
  ],
  "meta": {"repo": "git@...", "branch": "main"}
}
```

## Endpoints (exemplo)
- POST /api/v1/projects — cadastrar projeto
- POST /api/v1/projects/{id}/analyses — iniciar análise
- GET /api/v1/analyses/{id} — status/resultados
- GET /api/v1/analyses/{id}/report — baixar PDF

## Report Generator — estrutura do PDF
- Capa
- Sumário executivo (1 página)
- Visão geral do sistema
- Principais findings (TOP 10)
- Roadmap priorizado
- Anexo técnico (outputs das ferramentas)

## Backlog MVP (resumo por semana)
Semana 1: Landing, proposta, infra local (Postgres, Redis, MinIO), API bootstrap
Semana 2: Git Connector, wrappers SpotBugs/PMD/Checkstyle, Report Generator minimal
Semana 3: AI Orchestrator, agentes Java Specialist + Tech Lead, dashboard simples
Semana 4: GitHub App, testes end-to-end, prospecção e primeiras auditorias

## Métricas de sucesso
- Time to value < 48h
- 5 auditorias pagas nos primeiros 30 dias
- CSAT/NPS >= 8/10 nas primeiras entregas

## Riscos e mitigação
- Dependência de provedores de IA — abstração e limites de uso
- Privacidade do código — MSA/NDA, execução on-prem or VPC
- Falsos positivos — revisão humana final

## Próximos passos
1. Validar escopo e priorização dos agentes
2. Gerar repositório bootstrap (backend + frontend + infra)
3. Implementar MVP mínimo e executar primeiras 3 auditorias pagas

---
Documento gerado automaticamente (versão inicial). Revisões e detalhes adicionais podem ser adicionados em `docs/`.

