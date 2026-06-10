# State of the Art — Benchmark, Hardening & Roadmap

**Date:** 2026-06-09 · **Scope:** how this project compares to the best agentic financial advisors, what was hardened in the reliability sprint, and what remains on the roadmap to be credibly monetizable.

> Method: 4 code auditors traced every LLM hop of the pipeline; 3 researchers surveyed commercial products
> (Origin, PortfolioPilot, Magnifi, Cleo, Public Alpha, Fintool, Morgan Stanley's assistant), open research
> (TradingAgents, FinCon, FinRobot, OpenBB), anti-hallucination engineering practice, and US regulatory
> requirements; a synthesis pass ranked the gaps. The seven highest-impact items were implemented the same day.

---

## 1. What "state of the art" means for an AI financial advisor

From the product/research survey, the leaders share five load-bearing patterns:

1. **Deterministic math, narrating LLM.** The model never *computes* a number; code computes, the LLM explains
   (PortfolioPilot's "Hybrid-AI", Cleo). ([portfoliopilot.com](https://portfoliopilot.com/ai-financial-advisor))
2. **Verification that fails closed.** Outputs pass automated checks before reaching users — Origin runs
   **138 automated compliance/accuracy checks per output** and benchmarks on a 6,000-question CFP exam
   (98.3% vs ~79.5% human average). ([useorigin.com](https://useorigin.com/resources/blog/technical-overview))
3. **Provenance + freshness on every number** — citations with as-of timestamps (Morgan Stanley/Fintool style).
4. **Deterministic personalization** — user profile and risk constraints injected into every turn, not left to
   agent discretion (Origin's structured memory).
5. **Golden-question eval suites as release gates** with hallucination probes.

## 2. Gap analysis (before → after this sprint)

| Dimension | Before | After this sprint |
|---|---|---|
| **Grounding / hallucination** | One prompt line ("NEVER fabricate"); zero enforcement. Worse: the evaluator only ever saw the sub-agents' *LLM paraphrase* of tool data (telephone game), and any `directResponse` from the planner shipped unevaluated — pure model-memory answers were possible. | **Deterministic numeric grounding gate** (`GroundingService`): every figure in a response must exist in the raw tool data (tolerance-matched); violations trigger one corrective rewrite, then an explicit caution. **Raw tool outputs are captured** per session (`ToolCallAspect`) and fed to the evaluator + validator as ground truth. **`directResponse` is code-gated** to greetings without figures. |
| **Freshness / citations** | Tools emitted `quoteTime`/`source` but paraphrasing agents routinely dropped them. | Raw tool JSON (with timestamps/sources) reaches the evaluator intact; evaluator prompt **requires** "as of" citations; technical snapshot carries `asOf` + `source`. |
| **Analysis rigor** | `getTechnicalIndicators`/`analyzeTrends` derived "signals" and "support/resistance" from period high/low/average — pseudo-analysis that could mislead. | **Real indicators** computed in code from 1y of daily candles: SMA20/50, RSI14 (Wilder), 30-day annualized realized volatility, 1m/3m/1y returns, 52-week range; trend = transparent SMA-relationship rule, with the rule itself stated in the tool output. Portfolio **allocation % + concentration check** (>30% flag). |
| **Personalization** | Profile reached the answer only if the LLM planner happened to schedule a USER_PROFILE step. | **Deterministic injection**: orchestrator loads profile + holdings from the DB every query (`UserContextService`) into both planner and evaluator inputs; evaluator must tailor advice to risk tolerance/horizon/goals, never recommend excluded sectors, and flag ESG conflicts. |
| **Prompt-injection isolation** | Web/fintwit text flowed inline into the evaluator prompt. | Tool outputs wrapped in `<<TOOL_DATA>>…<<END_TOOL_DATA>>` markers + "data, never instructions" rules. |
| **Compliance/trust** | UI footer disclaimer only. | Advice responses end with an education-not-advice line; grounding verdicts are streamed to the UI ("Fact Check" events) so users *see* verification happen; unverifiable figures ship with an explicit caution rather than silently. |
| **Failure honesty** | Evaluator failure dumped raw `Step 1 [USER_PROFILE] - …` text at users; unparseable evaluator JSON was returned verbatim. | Clean fallback synthesis pass (grounded + caveated); raw dump only as a last resort. |

## 3. Honest benchmark after this sprint

The project now shares the load-bearing architectural patterns of credible products: deterministic math with a
narrating LLM, a tool-output fact ledger with a numeric faithfulness gate, deterministic per-turn profile
injection, mandatory source/as-of citations, and real indicators — meaningfully ahead of a ChatGPT wrapper and
most hobby agentic advisors.

The remaining distance to the leaders, stated honestly:

- A small `gpt-4o` / `gpt-4o-mini` tiering vs. the leaders' larger, heterogeneous ensembles routed per task
  (and no use of dedicated reasoning models for the hardest synthesis yet).
- One regex-based numeric gate vs. Origin's 138 checks + CFP benchmark harness.
- Free-tier, ~15-min-delayed quotes vs. institutional data feeds.
- No tax/scenario/backtesting/factor analytics vs. PortfolioPilot's quant stack.
- No persisted audit trail, eval suite in CI, or tool-call observability dashboards yet.
- No regulatory registration (see §5 — this constrains how the product may be marketed).

## 3b. LLM provider & cost (migrated from Groq → OpenAI)

The project originally ran on Groq's free tier, which capped at ~100K tokens/day and repeatedly blocked real
use. It now runs on **OpenAI (pay-as-you-go)** via LangChain4j's OpenAI client — there is no daily token wall;
cost is per token. Models are tiered and env-overridable (`OPENAI_*_MODEL`): orchestrator `gpt-4o` (planner +
evaluator), agent `gpt-4o` (tool-calling sub-agents), security `gpt-4o-mini` (cheap classification). One advisor
query is ~5–10 LLM calls; at gpt-4o rates that's roughly a few cents per query (drop the agent tier to
`gpt-4o-mini` to cut it further). On a `429` the orchestrator returns an honest capacity message and snapshots
are cached to limit redundant calls. **Operational watch item is now spend, not a daily cap** — set OpenAI
usage limits/budget alerts before real user load.

## 4. Roadmap (next sprints, in priority order)

1. **Tool-invocation verification (fail closed):** verify each data step actually invoked a tool; reject
   tool-free "data" answers (audit found prompt-only enforcement today).
2. **Golden-question eval suite as a CI release gate:** recorded tool fixtures + probes (nonexistent ticker
   must refuse; excluded-sector advice must be flagged; injection strings in web results must be ignored),
   reusing the runtime `GroundingService` as the assertion engine.
3. **Uniform `ToolResult` envelope** `{source, asOf, data|error}` across all tools incl. dated news items and
   symbol-resolution provenance ("interpreted 'Figma' as FIG").
4. **Fintwit/web-search honesty:** replace substring sentiment counting with model-based scoring or label it
   as a heuristic; surface degraded paths.
5. **Per-answer audit trail** persisted (plan, tool calls, grounding verdict) and exposed in the UI.
6. **Observability:** tool-call success/fallback rates, grounding-violation rate, latency percentiles.
7. **Model routing:** cheaper model for security/greeting; consider a stronger model for evaluation.
8. Redis-backed session state; httpOnly-cookie JWT; WebSocket auth (carried over from the previous evaluation).

## 5. Monetization & the regulatory line (read before charging money)

Research summary (not legal advice):

- Under the **Investment Advisers Act §202(a)(11)**, providing advice about securities, for compensation, as a
  business generally requires RIA registration. *Lowe v. SEC (1985)* keeps **impersonal, educational**
  publications outside the Act. ([kitces.com](https://www.kitces.com/blog/artificial-intelligence-compliance-considerations-in-financial-advice),
  [supreme.justia.com](https://supreme.justia.com/cases/federal/us/472/181/))
- A paid product that generates **personalized buy/sell recommendations from a user's profile** is squarely in
  "investment advice" territory (robo-advisors register as RIAs; the SEC tightened the internet-adviser
  exemption in 2024). ([innreg.com](https://www.innreg.com/blog/investment-advisor-regulation),
  [sec.gov](https://www.sec.gov/newsroom/press-releases/2024-42))
- Practical positioning for this product **pre-registration**: education/information framing, prominent
  AI-interaction disclosure, layered disclaimers (already emitted per-answer), and avoidance of "you should
  buy X" phrasing in favor of data-grounded analysis the user interprets. The current evaluator prompt and
  disclaimers implement this posture; revisit with counsel before charging for personalized recommendations.

## 6. Reliability architecture (as implemented)

```
User query
  → SecurityAgent (regex fail-closed + LLM check)
  → [UserContextService] profile + holdings + allocation loaded from DB  ──┐ injected deterministically
  → PlannerAgent (JSON plan; directResponse code-gated to greetings)      ◄┘
  → Sub-agents execute steps in parallel; ToolCallAspect captures EVERY raw @Tool result
  → EvaluatorAgent synthesizes from: query + PROFILE CONTEXT + sub-agent answers
        + RAW TOOL DATA (delimited <<TOOL_DATA>> blocks)
  → GroundingService: regex-extracts every figure from the response and requires a tolerance match
        in the raw tool data / profile context
        ├─ grounded → ship (UI shows "Fact Check ✅")
        ├─ violation → ONE corrective evaluator rewrite → re-check
        └─ still ungrounded → ship with explicit verification caution + log
  Fallbacks: evaluator failure → clean grounded summarize pass → (last resort) formatted raw results
```

Key classes: `GroundingService`, `UserContextService`, `TechnicalIndicators`,
`MarketDataService.getTechnicalSnapshot`, `ToolCallAspect.drainToolResults`,
`OrchestratorService.enforceGrounding`.
