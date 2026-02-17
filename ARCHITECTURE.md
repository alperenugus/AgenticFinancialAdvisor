# System Architecture (Current)

Last updated: 2026-02-17

This document describes the **current production architecture**.
For the exact agent count and role list, see `docs/AGENT_INVENTORY.md`.

## 1) High-Level Architecture

```
Frontend (React) -> REST/WebSocket -> Spring Boot Backend -> Groq LLMs + Finnhub + PostgreSQL
```

### Backend layers

1. **Security Layer**
   - Google OAuth2 authentication
   - JWT validation
   - Input threat screening via `SecurityAgent`

2. **Orchestration Layer**
   - `OrchestratorService` controls request lifecycle
   - Plan -> Execute -> Evaluate -> Retry flow
   - Delegates to specialist agents through tool calls

3. **Specialist Layer**
   - `UserProfileAgent`
   - `MarketAnalysisAgent`
   - `WebSearchAgent`
   - `FintwitAnalysisAgent`

4. **Symbol Resolution Consensus Layer**
   - `LlmTickerResolver` with planner/selector/evaluator/auditor roles
   - Consumes live candidate symbols from Finnhub search/quote data

5. **Data & Integration Layer**
   - `MarketDataService` (Finnhub quote/candle/news/profile/search)
   - PostgreSQL repositories for user/profile/portfolio data

## 2) Agent Topology

Total active LLM roles: **12**

- **Orchestration/governance (3)**
  - OrchestratorAgent
  - ResponsePlannerAgent
  - ResponseEvaluatorAgent

- **Core specialists (5)**
  - UserProfileAgentService
  - MarketAnalysisAgentService
  - WebSearchAgentService
  - FintwitAnalysisAgentService
  - SecurityValidator

- **Ticker-resolution consensus (4)**
  - SymbolResolutionPlannerAgent
  - SymbolSelectionAgent
  - SymbolSelectionEvaluatorAgent
  - SymbolSelectionAuditorAgent

## 3) Request Flow

1. Client sends `/api/advisor/analyze`
2. `SecurityAgent` validates query safety
3. `OrchestratorService` creates/updates plan
4. Orchestrator delegates to one or more specialist agents
5. Tool evidence is captured
6. `ResponseEvaluatorAgent` judges quality/grounding
7. If needed, orchestrator retries with evaluator feedback
8. Final response sent via REST + WebSocket update streams

## 4) Symbol Resolution Flow

1. `MarketDataService` builds candidate symbols from Finnhub search and quote probes
2. `LlmTickerResolver` planner produces disambiguation notes
3. Selector chooses symbol candidate
4. Evaluator validates identity correctness
5. Independent auditor re-validates selection
6. If rejected, resolver retries (bounded by config)
7. If consensus fails, resolver returns no symbol instead of guessing

## 5) LLM Model Routing

- **Orchestrator/evaluator/planner roles**: primary orchestrator model
- **Specialist and fast utility roles**: agent model

Configured in:
- `backend/src/main/java/com/agent/financialadvisor/config/LangChain4jConfig.java`
- `backend/src/main/resources/application.yml`

## 6) Key Configuration

- `agent.timeout.orchestrator-seconds`
- `agent.timeout.tool-call-seconds`
- `agent.evaluation.max-attempts`
- `market-data.finnhub.*`
- `market-data.symbol-resolution.max-attempts`

## 7) Testing Strategy (Current)

Targeted tests for agentic behavior:

- `EvaluatorDecisionParserTest`
- `LlmTickerResolverTest`
- `MarketDataServiceSymbolSelectionTest`
- `MarketAnalysisAgentTest`

Note: Some legacy controller-context tests currently fail due unrelated JWT test wiring (`JwtService` bean in WebMvc test contexts).

