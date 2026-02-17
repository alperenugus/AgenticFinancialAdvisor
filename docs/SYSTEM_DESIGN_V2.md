# System Design V2 (Current Agentic Design)

Last updated: 2026-02-17

This design reflects the current implementation and supersedes earlier drafts.

## Core Principles

1. **Agentic control loop**: plan -> execute -> evaluate -> retry
2. **Evidence-grounded answers**: responses validated against delegated tool evidence
3. **Consensus for symbol resolution**: planner + selector + evaluator + auditor
4. **Fail-safe behavior**: return no symbol instead of guessing when confidence is insufficient

## Runtime Agent Groups

- **Orchestration/governance**: 3 roles
- **Specialist domain agents**: 5 roles
- **Ticker-resolution consensus**: 4 roles
- **Total**: 12 LLM roles

Reference: `docs/AGENT_INVENTORY.md`

## Orchestrator Flow

1. Query enters `OrchestratorService`
2. Security validation gate runs
3. Planner generates execution plan
4. Orchestrator delegates to specialist agents
5. Evaluator checks final response quality
6. Retry executes if evaluator fails (bounded attempts)

## Symbol Resolution Flow

1. `MarketDataService` collects live candidates from Finnhub
2. `LlmTickerResolver` planning role creates disambiguation notes
3. Selection role proposes symbol
4. Evaluator role validates match quality
5. Auditor role performs independent check
6. On disagreement/failure, retry; otherwise accept

## Data Sources

- Finnhub quote/candle/news/profile/search endpoints
- Optional web/news providers through web-search agent tools
- PostgreSQL for user/profile/portfolio state

## Quality & Validation

- Evaluator and auditor outputs are expected in structured JSON
- Invalid/unclear evaluator outputs are treated as failures in ticker consensus
- Targeted tests validate parser behavior, resolver retries, and market-agent integration

