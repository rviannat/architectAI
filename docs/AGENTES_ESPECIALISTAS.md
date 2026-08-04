# Agentes Especialistas - Architect AI

Atualizado em: 2026-08-04

Este documento acompanha o status dos agentes especialistas combinados para o `Architect AI`.
Sempre que um novo agente for criado/alterado, este arquivo deve ser atualizado.

## Visao geral

- Orquestrador: `AIOrchestrator` + `DefaultAIOrchestrator`
- Consolidacao final: `TechLeadAgent`
- Contrato comum: `SpecialistAgent`
- Base compartilhada para prompts/contexto: `AbstractLlmSpecialistAgent`
- Parser padrao de resposta JSON: `AgentResponseParser`
- Cliente de LLM (multi-provider): `LlmClient` + `DefaultLlmClient`

## Status dos 7 agentes

- [x] 1. Software Architect
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/SoftwareArchitectAgent.java`
  - Foco: DDD, SOLID, Clean/Hexagonal, modularizacao, divida tecnica estrutural.

- [x] 2. Java Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/JavaSpecialistAgent.java`
  - Foco: Java 21, Spring Boot, JPA/Hibernate, transacoes, concorrencia, APIs REST.

- [x] 3. Security Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/SecuritySpecialistAgent.java`
  - Foco: OWASP Top 10, autenticacao/autorizacao, segredos, criptografia.

- [x] 4. Performance Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/PerformanceSpecialistAgent.java`
  - Foco: CPU, memoria, GC, SQL, cache, throughput.

- [x] 5. DevOps Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/DevOpsSpecialistAgent.java`
  - Foco: Docker, Kubernetes, CI/CD, observabilidade, IaC.

- [x] 6. Database Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/DatabaseSpecialistAgent.java`
  - Foco: modelagem, indices, queries, normalizacao, particionamento.

- [x] 7. Tech Lead (Orquestrador)
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/TechLeadAgent.java`
  - Foco: consolidar saidas, remover duplicidades, priorizar e gerar relatorio unico.

## Agente adicional (expansao)

- [x] 8. Quality Control Specialist
  - Classe: `backend/src/main/java/com/architectai/backend/ai/impl/agents/QualityControlSpecialistAgent.java`
  - Foco: qualidade de codigo, legibilidade, duplicacao, testabilidade, manutenibilidade.

## Endpoint de verificacao

- `GET /api/v1/agents`
  - Controller: `backend/src/main/java/com/architectai/backend/controller/AgentController.java`
  - Retorna especialistas registrados e referencia ao Tech Lead.

## Proximo padrao de atualizacao

Sempre que criar ou alterar agente, atualizar:

1. Esta secao de status (`[x]` / `[ ]`).
2. Caminho da classe.
3. Foco tecnico do agente.
4. Data no topo do documento.

## Historico de atualizacoes

- 2026-08-04 17:05
  - Criado o conjunto de 7 agentes combinados (6 especialistas + Tech Lead).
  - Adicionado endpoint `GET /api/v1/agents` para auditoria da arquitetura ativa.
  - Ativada geracao de relatorio individual por agente + relatorio geral consolidado.

- 2026-08-04 17:12
  - Adicionado agente `Quality Control Specialist` para elevar maturidade de auditoria de qualidade de codigo.

- 2026-08-04 17:23
  - Ajustado prompt do `Quality Control Specialist` para foco comercial (impacto em custo, risco e velocidade de entrega).

