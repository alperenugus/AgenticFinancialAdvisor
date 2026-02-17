# Agent Inventory (Current Runtime)

Last updated: 2026-02-17

This file is the source of truth for active LLM agent roles in the backend.

## Total LLM Agent Roles: 12

## A) Orchestration Layer (3)

1. **OrchestratorAgent**  
   - Entry coordinator for `/api/advisor/analyze`
   - Delegates to specialist agents and synthesizes final response

2. **ResponsePlannerAgent**  
   - Produces execution plans for orchestrator turns
   - Supports plan -> execute -> verify flow

3. **ResponseEvaluatorAgent**  
   - Judges orchestrator outputs against evidence
   - Triggers retry/correction when response quality is insufficient

## B) Core Specialist Layer (5)

4. **UserProfileAgentService** (`UserProfileAgent`)  
   - Profile, preferences, portfolio retrieval

5. **MarketAnalysisAgentService** (`MarketAnalysisAgent`)  
   - Market quotes, trend analysis, technical indicators

6. **WebSearchAgentService** (`WebSearchAgent`)  
   - Financial web/news research

7. **FintwitAnalysisAgentService** (`FintwitAnalysisAgent`)  
   - Social sentiment and fintwit-style analysis

8. **SecurityValidator** (`SecurityAgent`)  
   - Prompt-injection and malicious input screening

## C) Symbol Resolution Consensus Layer (4)

9. **SymbolResolutionPlannerAgent** (`LlmTickerResolver`)  
   - Builds symbol-disambiguation notes from user input + candidate list

10. **SymbolSelectionAgent** (`LlmTickerResolver`)  
   - Selects a candidate ticker symbol

11. **SymbolSelectionEvaluatorAgent** (`LlmTickerResolver`)  
   - Evaluates selection quality and correctness

12. **SymbolSelectionAuditorAgent** (`LlmTickerResolver`)  
   - Independent second-pass audit (balance check)

## Runtime Summary

- **Core business specialists**: 5
- **Orchestration/governance roles**: 3
- **Ticker-resolution consensus roles**: 4
- **Total active LLM roles**: **12**

