# Agentes Executivos - Architect AI

Atualizado em: 2026-08-04

Objetivo: complementar os agentes tecnicos com um squad executivo/comercial e gerar pacote de relatorio comercial separado do tecnico.

## Status dos agentes executivos no backend

- [x] 1) Proposal Architect
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/ProposalArchitectAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: transformar diagnostico tecnico em proposta com fases 30/60/90 dias.

- [x] 2) Revenue Strategist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/RevenueStrategistAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: converter risco tecnico em receita incremental, margem e estrategia de expansao.

- [x] 3) Pricing & Packaging Analyst
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/PricingPackagingAnalystAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: estruturar precificacao por valor e pacotes comercializaveis.

- [x] 4) Executive Account Manager
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/ExecutiveAccountManagerAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: manter alinhamento C-level e plano de sucesso 30/60/90 dias.

- [x] 5) Board Reporting Agent
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/BoardReportingAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: gerar narrativa para diretoria/investidores com KPIs executivos.

- [x] 6) Commercial Tech Lead (orquestrador executivo)
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/CommercialTechLeadAgent.java`
  - Dominio: `COMMERCIAL`
  - Missao: consolidar narrativa C-level e roadmap comercial.

## Relatorios executivos

- Master tecnico: `report_master_technical_<analysisId>_<timestamp>.pdf`
- Master comercial: `report_master_commercial_<analysisId>_<timestamp>.pdf`
- Manifest: `manifest_<analysisId>.md`
- Endpoint: `GET /api/v1/analyses/{id}/reports`

## Proxima esteira sugerida

- [ ] Pipeline & Forecast Analyst
- [ ] Customer Success Planner
- [ ] Compliance & Contract Agent

## KPIs recomendados

- Receita incremental por recomendacao tecnica implementada.
- Reducao de incidentes (risco) apos execucao do roadmap.
- Lead time de entrega antes/depois das melhorias.
- Margem por projeto e taxa de conversao proposta -> contrato.
- Expansao de conta (upsell/cross-sell) em 90 dias.

