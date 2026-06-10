# CLAUDE.md — Agentic Financial Advisor

Project-level operating manual for Claude Code. Read this before touching anything.
The user's global rules (`~/.claude/CLAUDE.md` §1–5: Think Before Coding, Simplicity First,
Surgical Changes, Goal-Driven Execution, Deploy Verification) are the floor — this file adds to them.

---

## 0. Standing instructions from the owner (Alperen)

These are durable directives for this repo. Honor them on every task unless told otherwise:

1. **Test on production with Playwright MCP.** After any user-facing change, deploy and verify it on the
   live Railway URLs using the `playwright` MCP browser tools — not just locally. A fake test user exists
   for sign-in (see §6). Real prod verification > local "it builds."
2. **Railway is the deploy target. Use the Railway CLI.** Everything (backend, frontend, Postgres) runs on
   Railway. Deploy with `railway up`, verify with `railway deployment list` / `railway logs`. Never claim a
   deploy succeeded without confirming `SUCCESS` + healthy startup logs (global rule §5).
3. **The product must retrieve the LATEST market & stock information.** Live quotes and recent news are a
   core requirement, not a nice-to-have. Any change near the agents/market-data must preserve real-time
   data retrieval and surface freshness (timestamps) to the user. Never let it regress to stale/cached/mock data.
4. **Auth: support both Google sign-in AND email/password.** Keep both paths working.
5. **Keep all documentation updated.** When you change behavior, update `README.md`, `ARCHITECTURE.md`,
   `API.md`, `DEPLOYMENT.md`, and the relevant files under `docs/` in the same pass.
6. **Work autonomously and use best judgment.** The owner is frequently AFK. Prefer shipping a verified,
   well-reasoned change over stopping to ask, unless a decision is irreversible or genuinely ambiguous.

---

## 1. What this is

An **agentic AI financial advisor**: a multi-agent system that answers natural-language finance questions
(stock analysis, portfolio guidance, market sentiment) using live market data and an LLM-driven
**Plan → Execute → Evaluate** loop. It is an educational/demo system, not licensed financial advice
(disclaimer rendered in the UI footer).

**Production URLs**
- Frontend: https://agenticfinancialadvisorfrontend-production.up.railway.app/
- Backend:  https://agenticfinancialadvisorbackend-production.up.railway.app/
- GitHub:   https://github.com/alperenugus/AgenticFinancialAdvisor

---

## 2. Tech stack

**Backend** (`backend/`) — Spring Boot 3.4.0, Java 21, Maven
- LangChain4j 0.34 (`langchain4j` + `langchain4j-open-ai`) talking to **Groq** via its OpenAI-compatible API
  (models: `llama-3.3-70b-versatile` for reasoning, `llama-3.1-8b-instant` for fast tool calls).
- Spring Security + OAuth2 client (Google) + **JWT** (jjwt 0.12.3), stateless sessions.
- Spring Data JPA / Hibernate; **PostgreSQL** in prod, **H2** in test/local.
- Spring WebFlux `WebClient` for outbound HTTP (market data, web search).
- WebSocket (STOMP over SockJS) for the live "agent thinking" stream.
- Bucket4j (token-bucket) rate limiting; Spring AOP (`ToolCallAspect`) to stream tool calls to the UI.

**Frontend** (`frontend/`) — React 18, Vite 5, Tailwind 3
- `axios` (REST), `@stomp/stompjs` + `sockjs-client` (WebSocket), `recharts` (charts), `lucide-react` (icons).
- Context-based auth (`AuthContext`), JWT stored in `localStorage`.

**Infra** — Railway: 3 services in project `AgenticFinancialAdvisor` →
`AgenticFinancialAdvisorBackend`, `AgenticFinancialAdvisorFrontend`, `Postgres`.

---

## 3. Architecture (how a request flows)

```
User → Frontend (React) ──REST POST /api/advisor/analyze (Bearer JWT)──► AdvisorController
                          └─WebSocket /ws (STOMP) ◄── live agent activity ──┘
AdvisorController → OrchestratorService.coordinateAnalysis(userId, query, sessionId)
   1. SecurityAgent.validateInput()          (deterministic + LLM guard against off-topic/injection)
   2. PlannerAgent.createPlan()               → JSON plan {queryType, directResponse?, steps[]}
   3. Executor runs steps IN PARALLEL (CompletableFuture + fixed thread pool), routed by agent name:
        MARKET_ANALYSIS → MarketAnalysisAgent (Finnhub via MarketDataService)
        USER_PROFILE    → UserProfileAgent     (DB: profile + portfolio)
        WEB_SEARCH      → WebSearchAgent        (Tavily — latest news/info)
        FINTWIT         → FintwitAnalysisAgent  (social sentiment; web-search fallback)
   4. EvaluatorAgent.evaluate()               → {verdict: PASS|RETRY, response, feedback}
   5. RETRY loops back to PLAN (max 2 retries); falls back to raw results if the evaluator fails.
```

Key orchestration facts (see `service/orchestrator/OrchestratorService.java`):
- `MAX_PLAN_RETRIES=2`, `MAX_PLAN_STEPS=4`, `MAX_CONVERSATION_HISTORY=5` (in-memory per session).
- Overall timeout `agent.timeout.orchestrator-seconds` (default 90s); per-step timeout ≈ `tool-call * 3`.
- Conversation history + session→userId are **in-memory `ConcurrentHashMap`s** (lost on restart, not shared
  across instances — single-instance assumption).
- Agent activity is pushed to the UI via `WebSocketService.sendAgentActivity()`.

**Reliability invariants (do NOT weaken these — see `docs/STATE_OF_THE_ART.md`):**
- **Grounding gate**: `GroundingService` + `OrchestratorService.enforceGrounding` verify every figure in a
  response against raw tool data; `ToolCallAspect` captures raw `@Tool` results per session for this purpose.
  Never bypass the gate or return LLM text to users without it.
- **`directResponse` is code-gated** to GREETING plans with no figures — factual answers must come from tools.
- **Deterministic personalization**: `UserContextService` injects profile + allocation into planner AND
  evaluator inputs on every query. Don't make personalization depend on the LLM choosing a profile step.
- **Indicators are computed in code** (`util/TechnicalIndicators`, `MarketDataService.getTechnicalSnapshot`)
  — the LLM narrates numbers, it never computes them.
- Tool outputs are wrapped in `<<TOOL_DATA>>` markers in evaluator input (prompt-injection isolation).

---

## 4. Repo layout

```
backend/
  src/main/java/com/agent/financialadvisor/
    config/        SecurityConfig, JwtAuthenticationFilter, OAuth2Config, CorsFilter, WebConfig,
                   WebSocketConfig, WebClientConfig, ProxyConfig, RateLimitConfig, DataSourceConfig,
                   LangChain4jConfig
    controller/    AdvisorController, AuthController, PortfolioController, UserProfileController
    service/       JwtService, MarketDataService, RateLimitService, WebSocketService,
                   CustomOAuth2UserService, OAuth2AuthenticationSuccessHandler
      agents/      PlannerAgent, EvaluatorAgent, MarketAnalysisAgent, FintwitAnalysisAgent,
                   WebSearchAgent, UserProfileAgent, SecurityAgent
      orchestrator/OrchestratorService
    aspect/        ToolCallAspect          (streams @Tool calls to the WebSocket)
    util/          ReActParser, SecurityUtil
    model/         User, UserProfile, Portfolio, StockHolding, Recommendation
    repository/    *Repository (Spring Data JPA)
  src/main/resources/application.yml, application-local.yml
  src/test/...     (controller + agent + orchestrator tests, run on H2)
  Dockerfile, railway.json, railway.toml, nixpacks.toml, pom.xml
frontend/
  src/App.jsx, main.jsx, index.css
    components/  LoginPage, ChatComponent, PortfolioView, UserProfileForm, OnboardingWizard, AgentThinkingPanel
    contexts/    AuthContext, AuthContextObject, useAuth
    services/    api.js (axios), websocket.js (STOMP)
  vite.config.js, tailwind.config.js, package.json
docs/            DATA_FRESHNESS, ENVIRONMENT_VARIABLES, GOOGLE_AUTH_*, HOW_TO_*, RUN_LOCALLY,
                 SYSTEM_DESIGN_V2, VALIDATION_GUIDE, railway/*, troubleshooting/*
README.md  ARCHITECTURE.md  API.md  DEPLOYMENT.md  PROJECT_EVALUATION.md
```

---

## 5. Build / run / test

### Backend
```bash
cd backend
mvn clean package                 # full build + tests (what CI-equivalent should run)
mvn spring-boot:run -Dspring-boot.run.profiles=local   # run locally on H2 (no Postgres needed)
mvn test                          # tests only (H2, application-test.yml)
```
Local run uses `application-local.yml` (H2 in-memory, console at `/h2-console`). Provide LLM/market keys
via env vars or it degrades gracefully (LLM calls fail, market data returns "not configured").

### Frontend
```bash
cd frontend
npm install
npm run dev        # Vite dev server on :5173, proxies /api and /ws to localhost:8080
npm run build      # production build → dist/   (this is what Railway runs)
npm run lint       # eslint, --max-warnings 0
```
Backend base URL for the frontend is `VITE_API_BASE_URL` (defaults to `http://localhost:8080/api`).

### ⚠️ Match the platform build before deploying (global rule §5)
Railway builds the **backend** with the multi-stage `Dockerfile` →
`mvn clean package -DskipTests`. `-DskipTests` skips test **execution**, NOT test **compilation** — a broken
test file still fails the build. Run `mvn clean package` (tests on) locally before pushing.
The **frontend** is built by Railway via **Nixpacks auto-detection** (`npm ci && npm run build`, served as
static `dist/`). Run `npm run build` locally first.

---

## 6. Deployment — Railway (verify, never assume)

The repo links to Railway project `AgenticFinancialAdvisor`. If `railway status` shows a different project:
```bash
railway link --project AgenticFinancialAdvisor   # then pick environment "production"
```

**Deploy a service** (from repo root, with the right directory):
```bash
railway up --service AgenticFinancialAdvisorBackend  --path-as-root backend
railway up --service AgenticFinancialAdvisorFrontend --path-as-root frontend
```
(or `cd backend && railway up --service AgenticFinancialAdvisorBackend`).

**Then VERIFY — do not report "deployed" until all pass (global rule §5):**
```bash
railway deployment list --service AgenticFinancialAdvisorBackend     # latest row must be SUCCESS
railway logs --service AgenticFinancialAdvisorBackend                 # runtime logs
railway logs --service AgenticFinancialAdvisorBackend --build <id>    # build logs of a deploy
railway logs --service AgenticFinancialAdvisorBackend --deployment <id>
```
Scan startup logs for `WARN|ERROR|FATAL|fallback|missing|not configured`. A green build can still crash on boot.

### 🩺 The healthcheck rule (this has bitten us — do NOT regress it)
Railway's healthcheck path for the backend is a **public** endpoint that must return **HTTP 200 without auth**.
If a healthcheck path is behind Spring Security it returns **302 → /oauth2/authorization/google**, Railway
reads that as "service unavailable," and the deploy is marked **FAILED** while the previous build keeps
serving (silent staleness). The healthcheck endpoint (`/api/health`, and `/api/advisor/status`) is therefore
in the `permitAll()` list in `SecurityConfig`. **Never move it behind authentication.**

### Required environment variables (set on the backend service)
| Var | Purpose | Notes |
|---|---|---|
| `DATABASE_URL`, `SPRING_DATASOURCE_USERNAME/PASSWORD` | Postgres | injected by Railway |
| `JWT_SECRET` | HMAC key for JWTs | **MUST be set** to a strong ≥32-char secret. If unset, the app falls back to the placeholder committed in `application.yml` → anyone can forge tokens. |
| `GROQ_API_KEY` | LLM inference | required for the agents to work |
| `FINNHUB_API_KEY` | live quotes / company news | required for market data |
| `TAVILY_API_KEY` | web search (latest news/info) | required for "latest info" |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` / `GOOGLE_REDIRECT_URI` | Google OAuth | |
| `CORS_ORIGINS` | allowed frontend origins (comma-sep) | first entry is the OAuth redirect target |
| `NEWS_API_KEY`, `SERPER_API_KEY`, `TWITTER_BEARER_TOKEN` | optional providers | unset = those paths no-op/fallback |

Set/read vars:
```bash
railway variables --service AgenticFinancialAdvisorBackend            # list
railway variables --service AgenticFinancialAdvisorBackend --set 'JWT_SECRET=...'   # set (triggers redeploy)
```
Frontend needs `VITE_API_BASE_URL` = `https://agenticfinancialadvisorbackend-production.up.railway.app/api`
**at build time** (Vite inlines env vars during `npm run build`).

---

## 7. Testing on production with Playwright MCP

After deploying, drive the live site with the `playwright` MCP browser tools:
1. `browser_navigate` to the frontend URL.
2. Sign in with the **email/password test user** (preferred for automation — Google OAuth needs an interactive
   Google account and is hard to automate). Create one via the register form or `POST /api/auth/register`.
   Keep test creds out of git; note them in the deploy/PR description, not in committed files.
3. Exercise the core flows: onboarding/profile, portfolio add/remove, and the **AI Advisor chat** (ask a
   live-data question like "What's AAPL trading at right now?" and confirm the answer reflects a fresh quote +
   the agent-thinking stream renders).
4. Capture `browser_snapshot` / `browser_take_screenshot` and check `browser_console_messages` +
   `browser_network_requests` for errors.

---

## 8. Conventions & gotchas

- **Surgical changes only** (global §3). Match existing style. Don't refactor unrelated code.
- **Secrets never get committed.** `application.yml` ships safe placeholders + env-var indirection
  (`${VAR:default}`). Real keys live only in Railway. If you ever see a real key in a file, stop and flag it.
- **JPA `ddl-auto=update`** — schema auto-evolves by ADDING columns; it never drops/alters. New entity fields
  must be nullable or have defaults. There are no migration scripts (Flyway/Liquibase) — keep model changes
  additive.
- **LazyInitializationException** has bitten `UserProfile`/`Portfolio` holdings before (fixed). Be careful
  accessing lazy collections outside a transaction; agents run on background threads.
- **Agent name matching** is normalized (`MARKET_ANALYSIS`/`MarketAnalysis`/`market analysis` all map the
  same) in `OrchestratorService.executeAgentTask` — keep the planner's allowed agent names in sync.
- **In-memory session state** (conversation history, rate-limit buckets) assumes a single backend instance.
  Don't scale to >1 replica without externalizing it (Redis).
- **LLM output is parsed as JSON** via `extractJson` (handles markdown fences / surrounding prose). Keep
  planner/evaluator prompts emitting clean JSON.
- **CORS is configured in two places** (`SecurityConfig.corsConfigurationSource` and `WebConfig`) — change both
  consistently; both read `CORS_ORIGINS`.
- **Data freshness is a feature.** When editing `MarketDataService`/agents, preserve live fetches and surface
  the quote/news timestamp. See `docs/DATA_FRESHNESS.md`.

---

## 9. Definition of done for a change here

1. `mvn clean package` (backend) and `npm run build` + `npm run lint` (frontend) pass locally.
2. Deployed to Railway and `railway deployment list` shows `SUCCESS` for the new SHA.
3. Startup logs are clean of unexpected `WARN/ERROR/fallback/missing`.
4. Verified on the production URL with Playwright MCP (sign in, exercise the changed flow).
5. Docs updated (§0.5).
6. Committed with a clear message; PR if the owner uses the PR flow.
