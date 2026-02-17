# Agentic Financial Advisor - Current Plan

Last updated: 2026-02-17

This plan reflects the **current implemented architecture** (not the historical Ollama/Alpha Vantage prototype plan).

## 1) Current Runtime Architecture

The backend now runs a layered multi-agent system:

- **12 LLM agent roles total** (see `docs/AGENT_INVENTORY.md`)
  - 3 orchestration/governance
  - 5 core specialists
  - 4 ticker-resolution consensus roles

Core user-facing flow:

1. Security validation (`SecurityAgent`)
2. Orchestrator planning (`ResponsePlannerAgent`)
3. Specialist delegation (UserProfile / MarketAnalysis / WebSearch / Fintwit)
4. Response quality evaluation (`ResponseEvaluatorAgent`)
5. Retry/correction when needed

Ticker-resolution flow:

1. Candidate discovery from Finnhub (`MarketDataService`)
2. Planner -> Selector -> Evaluator -> Auditor (`LlmTickerResolver`)
3. Bounded retries for disambiguation

## 2) Current Tech Stack

- **Backend**: Spring Boot 3.4, LangChain4j 0.34.0, Java 21
- **LLMs**: Groq-compatible models (`llama-3.3-70b-versatile` + `llama-3.1-8b-instant`)
- **Data**: PostgreSQL, Finnhub market APIs
- **Frontend**: React + Vite
- **Auth**: Google OAuth2 + JWT

## 3) Near-Term Roadmap

### A. Reliability & Quality
- Expand evaluator/auditor test cases with replay-style fixtures
- Add explicit telemetry for planner/evaluator retry outcomes
- Add alerting for elevated resolver rejection rates

### B. Product Quality
- Improve user-facing error messaging when symbol confidence is insufficient
- Add transparent UI labels for evidence-backed responses
- Add richer "why this symbol" explanation cards

### C. Performance
- Introduce bounded caching where safe (non-price metadata only)
- Add timeout profiling dashboards by agent role

### D. Testing
- Keep targeted agent tests green in CI:
  - `EvaluatorDecisionParserTest`
  - `LlmTickerResolverTest`
  - `MarketDataServiceSymbolSelectionTest`
  - `MarketAnalysisAgentTest`

## 4) Documentation Governance

- `docs/AGENT_INVENTORY.md` is the source of truth for agent counts/roles
- `README.md`, `ARCHITECTURE.md`, and `API.md` must stay aligned with that inventory
- Legacy infrastructure docs are kept for historical reference unless explicitly migrated

