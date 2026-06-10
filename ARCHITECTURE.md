# System Architecture

## Overview

The Agentic Financial Advisor uses a **multi-agent architecture** where specialized AI agents collaborate to provide investment recommendations. The system is built on Spring Boot with LangChain4j for LLM integration, powered by OpenAI API for fast LLM inference. The application supports both Google OAuth2 and email/password authentication, issuing JWT tokens.

The system runs **7 agents** coordinated by a Plan-Execute-Evaluate orchestrator: **Planner**, **Evaluator**, **Security**, **UserProfile**, **MarketAnalysis**, **WebSearch**, and **Fintwit**.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│                                                                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐        │
│  │   React Frontend │  │  Mobile App      │  │  API Clients     │        │
│  │   (Vite + React) │  │  (Future)        │  │  (Third-party)    │        │
│  │  + Auth Context  │  │                  │  │                  │        │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘        │
│           │                      │                      │                   │
│           └──────────────────────┼──────────────────────┘                   │
│                                  │                                            │
│                    HTTP/REST + WebSocket (JWT Authenticated)                 │
└──────────────────────────────────┼────────────────────────────────────────────┘
                                   │
┌──────────────────────────────────┼────────────────────────────────────────────┐
│                         APPLICATION LAYER                                      │
│                                   │                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                    SECURITY LAYER                                      │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │  Spring Security (OAuth2 + email/password)                   │   │   │
│  │  │  - Google OAuth2 Authentication                              │   │   │
│  │  │  - Email/Password (BCrypt) Authentication                    │   │   │
│  │  │  - JWT Token Validation (JWT_SECRET required)                │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                   │                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REST Controllers                              │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │   │
│  │  │   Advisor    │  │   Portfolio  │  │ User Profile │             │   │
│  │  │  Controller  │  │  Controller │  │  Controller  │             │   │
│  │  │  (Auth'd)    │  │  (Auth'd)   │  │  (Auth'd)    │             │   │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘             │   │
│  └─────────┼─────────────────┼─────────────────┼───────────────────────┘   │
│            │                 │                 │                            │
│  ┌─────────▼─────────────────▼─────────────────▼───────────────────────┐  │
│  │                    WebSocket Service                                  │  │
│  │              (Real-time agent thinking updates)                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                   │                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │              ORCHESTRATOR SERVICE (Plan-Execute-Evaluate)             │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │  Planner + Evaluator + Security agents                       │   │   │
│  │  │  - Planner creates JSON execution plan                       │   │   │
│  │  │  - Executes plan steps in parallel                           │   │   │
│  │  │  - Evaluator synthesizes / requests retry                    │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └───────────┬───────────────────────────────────────────────────────────┘   │
│              │                                                                │
│  ┌───────────┴───────────────────────────────────────────────────────────┐  │
│  │                    AGENT LAYER (executed in parallel)                 │  │
│  │                                                                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│  │
│  │  │    User      │  │    Market    │  │     Web      │  │   Fintwit  ││  │
│  │  │   Profile    │  │   Analysis   │  │    Search    │  │  Analysis  ││  │
│  │  │    Agent     │  │    Agent     │  │    Agent     │  │    Agent   ││  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘│  │
│  │         │                 │                  │                │       │  │
│  │         └─────────────────┴──────────────────┴────────────────┘       │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                            │
│  ┌─────────────────────────────────▼──────────────────────────────────────┐  │
│  │                      SERVICE LAYER                                      │  │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │  │
│  │  │              MarketDataService                                   │  │  │
│  │  │  - Finnhub + Yahoo Finance (fallback) integration              │  │  │
│  │  │  - Stock price data                                             │  │  │
│  │  │  - Company fundamentals                                         │  │  │
│  │  │  - Market news                                                  │  │  │
│  │  └──────────────────────────────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                            │
└───────────────────────────────────┼────────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼────────────────────────────────────────────┐
│                            DATA LAYER                                          │
│                                    │                                            │
│  ┌─────────────────────────────────▼──────────────────────────────────────┐  │
│  │                    Repository Layer                                      │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │  │
│  │  │     User     │  │   User       │  │  Portfolio   │  │Recommendation│ │
│  │  │  Repository  │  │  Profile     │  │  Repository  │  │  Repository  │ │
│  │  │  (OAuth)     │  │  Repository  │  │              │  │              │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│  └─────────┼─────────────────┼─────────────────┼─────────────────┼──────────┘  │
│            │                 │                 │                 │              │
│  ┌─────────▼─────────────────▼─────────────────▼─────────────────▼──────────┐ │
│  │                    PostgreSQL Database                                   │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │ │
│  │  │    users     │  │   user_      │  │  portfolios  │  │recommendations│ │
│  │  │ (Google OAuth)│  │   profiles   │  │              │  │               │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL SERVICES                                    │
│                                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │    Google    │  │    OpenAI      │  │   Alpha     │  │   NewsAPI   │     │
│  │   OAuth2     │  │     API      │  │  Vantage    │  │  (Optional) │     │
│  │              │  │              │  │  (Market     │  │             │     │
│  │ Authentication│  │  LLM Inference│  │   Data)     │  │             │     │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Agent Workflow

### Request Flow: Plan-Execute-Evaluate Loop

```
User Query
    │
    ▼
┌─────────────────┐
│ AdvisorController│
│  /api/advisor/   │
│    analyze       │
└────────┬─────────┘
         │
         ▼
┌─────────────────┐
│ SecurityAgent    │──► Validates input (deterministic + LLM)
└────────┬─────────┘
         │ (if safe)
         ▼
┌─────────────────┐
│ PlannerAgent     │──► Analyzes query, creates JSON execution plan
│  (70B LLM)      │    (which agents, what tasks, what order)
└────────┬─────────┘
         │
         ├─► GREETING? → Return directResponse immediately
         │
         ▼ (has steps)
┌─────────────────┐
│ Executor         │──► Runs plan steps IN PARALLEL:
│ (Orchestrator)   │
│                  │    ├─► UserProfileAgent (70B LLM + tools)
│                  │    ├─► MarketAnalysisAgent (70B LLM + tools)
│                  │    ├─► WebSearchAgent (70B LLM + tools)
│                  │    └─► FintwitAnalysisAgent (70B LLM + tools)
└────────┬─────────┘
         │
         ▼
┌─────────────────┐
│ EvaluatorAgent   │──► Reviews results, synthesizes response
│  (70B LLM)      │
└────────┬─────────┘
         │
         ├─► PASS → Return synthesized response to user
         │
         └─► RETRY → Back to PlannerAgent with feedback
                      (max 2 retries, then return available data)
```

### Agent Architecture

```
┌─────────────────────────────────────────────────────────────┐
│           OrchestratorService (Plan-Execute-Evaluate)       │
│  - Runs the agentic loop                                    │
│  - No manual regex or hardcoded patterns                    │
│  - Self-correcting via Evaluator retry feedback             │
│  - Maintains conversation history for follow-ups            │
└───────────────┬─────────────────────────────────────────────┘
                │
    ┌───────────┼───────────────────────────────────┐
    │           │                                     │
    ▼           ▼                                     ▼
┌────────┐  ┌────────┐                          ┌────────┐
│Planner │  │Evaluator│                         │Security│
│ Agent  │  │ Agent   │                          │ Agent  │
│(70B)   │  │ (70B)   │                          │(70B)   │
└────────┘  └────────┘                          └────────┘
                │
                │ Plan steps executed in parallel
                │
    ┌───────────┼───────────┬───────────┐
    │           │           │           │
    ▼           ▼           ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ User  │  │ Market │  │   Web  │  │Fintwit │
│Profile│  │Analysis│  │ Search │  │Analysis│
│ Agent │  │ Agent  │  │ Agent  │  │ Agent  │
│(70B)  │  │ (70B)  │  │ (70B)  │  │ (70B)  │
└───┬───┘  └───┬────┘  └───┬────┘  └───┬────┘
    │           │            │           │
    └───────────┼────────────┼───────────┘
                │            │
                ▼            ▼
         ┌──────────────────────────────────┐
         │    MarketDataService            │
         │  (Finnhub API)                  │
         └──────────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────────┐
         │    PostgreSQL Database           │
         │  (User data, portfolios)         │
         └──────────────────────────────────┘
```

## Component Details

### 0. Reliability & Grounding Pipeline

**Purpose**: A financial advisor must never invent figures. Three deterministic (non-LLM) mechanisms backstop the prompts — see [docs/STATE_OF_THE_ART.md](./docs/STATE_OF_THE_ART.md) for the full design rationale.

1. **Raw tool-data capture** (`ToolCallAspect`): every `@Tool` result is captured per session and fed to the
   EvaluatorAgent as delimited `<<TOOL_DATA>>` blocks. Without this, the evaluator only sees the sub-agents'
   LLM paraphrases of tool output (a "telephone game" that loses provenance and invites drift).
2. **Numeric grounding gate** (`GroundingService` + `OrchestratorService.enforceGrounding`): every figure in a
   synthesized response must match a number present in the raw tool data or profile context (tolerance-matched;
   small counts/years whitelisted). Violations trigger one corrective evaluator rewrite; if figures remain
   unverifiable, the response ships with an explicit caution. Verdicts stream to the UI as "Fact Check" events.
3. **Deterministic personalization** (`UserContextService`): the user's profile (risk tolerance, horizon, goals,
   budget, preferred/excluded sectors, ESG flag) + holdings + allocation/concentration summary are loaded from
   the DB on every query and injected into BOTH the planner and evaluator inputs. Advice is tailored in the
   evaluator by prompt contract (never recommend excluded sectors, flag ESG conflicts, cite risk tolerance).

Additional code-level gates: the planner's `directResponse` is honored **only** for `GREETING` plans containing
no figures (otherwise a model-memory answer could ship unevaluated), and evaluator failures fall back to a
clean grounded summarization pass instead of dumping raw step output.

### 1. Orchestrator Service (Plan-Execute-Evaluate)

**Purpose**: Coordinates the Plan-Execute-Evaluate agentic loop. All query understanding is done by LLMs -- no regex patterns, no hardcoded greeting lists, no manual bypasses.

**Key Features**:
- **Plan-Execute-Evaluate loop** with self-correction (max 2 retries)
- **PlannerAgent** creates structured JSON execution plans from user queries
- **EvaluatorAgent** reviews results, synthesizes responses, or requests retry
- Parallel execution of plan steps via sub-agents
- **Deterministic profile injection** (UserContextService) into planner + evaluator on every query
- **Numeric grounding gate** on every synthesized response (GroundingService; raw tool data as ground truth)
- Conversation history (last 5 exchanges) for follow-up context
- Sends real-time updates via WebSocket at each phase (including grounding verdicts)
- Grounded fallback synthesis when the evaluator fails or retries exhaust

**Architecture**:
- **PlannerAgent** (gpt-4o): Analyzes query intent, creates structured plans
- **EvaluatorAgent** (gpt-4o): Reviews results, synthesizes or retries
- **Sub-agents**: Each has its own 70B LLM instance for tool calling
- **SecurityAgent**: Deterministic patterns + LLM validation (first gate)

**Plan Format** (produced by PlannerAgent):
```json
{
  "queryType": "STOCK_PRICE",
  "directResponse": null,
  "steps": [
    {"agent": "MARKET_ANALYSIS", "task": "Get current stock price for Apple"}
  ]
}
```

**Evaluation Format** (produced by EvaluatorAgent):
```json
{
  "verdict": "PASS",
  "response": "The current stock price for Apple (AAPL) is **$195.50** USD.",
  "feedback": null
}
```

### 2. Multi-Agent LLM Architecture

**Key Innovation**: Each agent has its own LLM instance, enabling independent reasoning and specialized behavior.

**Agent LLM Configuration**:
- All agents use the same `agentChatLanguageModel` bean (gpt-4o)
- Each agent maintains its own conversation memory per session
- Agents can reason independently before calling their tools

**Agent Tools Pattern**:

Each agent exposes tools via `@Tool` annotations that its own LLM can call:

```java
@Tool("Get user's investment profile...")
public String getUserProfile(String userId) {
    // Implementation
}
```

Each agent's LLM:
- Understands when to call its own tools
- Passes correct parameters
- Handles tool responses
- Can reason about tool results before responding

**Portfolio Access Tools** (UserProfileAgent):
- `getPortfolio(userId)` - Full portfolio with holdings, values, gain/loss
- `getPortfolioHoldings(userId)` - List of owned stocks
- `getPortfolioSummary(userId)` - Quick portfolio overview

These tools automatically refresh current stock prices when called, ensuring the AI always has up-to-date portfolio data.

**Benefits of Multi-Agent LLM Architecture**:
- **Separation of Concerns**: Each agent is independent with its own reasoning
- **Scalability**: Agents can be optimized or replaced independently
- **Modularity**: Easier to add new agents or modify existing ones
- **Better Coordination**: Orchestrator decides which agent(s) to use based on query

### 3. Data Flow

```
User Request
    │
    ▼
Controller (REST/WebSocket)
    │
    ▼
OrchestratorService
    │
    ├─► SecurityAgent validates input
    │
    ├─► PlannerAgent (70B) creates execution plan
    │   └─► Returns JSON: queryType, steps[], or directResponse
    │
    ├─► For each step (in parallel):
    │   ├─► UserProfileAgent (70B) → tools → Database
    │   ├─► MarketAnalysisAgent (70B) → tools → Finnhub API
    │   ├─► WebSearchAgent (70B) → tools → Tavily/Serper API
    │   └─► FintwitAnalysisAgent (70B) → tools → Twitter/Web Search
    │
    ├─► EvaluatorAgent (70B) reviews results
    │   ├─► PASS → Synthesized response
    │   └─► RETRY → Back to PlannerAgent with feedback
    │
    ▼
Final Response (via WebSocket + REST)
```

## Technology Stack

### Backend Framework
- **Spring Boot 3.4**: Application framework
- **Spring Data JPA**: Database access
- **Spring WebSocket**: Real-time communication
- **Spring WebFlux**: Reactive HTTP client

### LLM Integration
- **LangChain4j 0.34.0**: Java LLM framework
- **OpenAI API**: Fast LLM inference
  - **All agents**: gpt-4o (Planner, Evaluator, and all sub-agents)
  - The 70B model is used universally for reliable tool calling on OpenAI's API

### Authentication & Security
- **Spring Security OAuth2**: Google Sign-In integration
- **JWT (JSON Web Tokens)**: Stateless authentication
- **OAuth2 Client**: Google OAuth2 flow

### Database
- **PostgreSQL**: Primary database
- **H2**: In-memory database (testing)

### External APIs
- **Finnhub**: Market data (free tier: 60 calls/min, 1,000,000 calls/month)
- **OpenAI API**: Fast LLM inference (gpt-4o)
- **Tavily/Serper**: Web search for financial news and analysis (optional)
- **NewsAPI**: Financial news (optional)

## Security Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Security Layers                     │
│                                                       │
│  1. Google OAuth2 Authentication                    │
│     - Secure passwordless authentication            │
│     - User identity verification                    │
│                                                       │
│  2. JWT Token Validation                            │
│     - Stateless authentication                      │
│     - Token expiration (24 hours)                    │
│     - Automatic token refresh                        │
│                                                       │
│  3. Spring Security Filter Chain                    │
│     - Protected endpoints                            │
│     - CORS with credentials                         │
│     - Request authentication                        │
│                                                       │
│  4. Input Validation (Spring Validation)            │
│  5. SQL Injection Prevention (JPA)                  │
│  6. Rate Limiting (Recommended)                     │
└─────────────────────────────────────────────────────┘
```

### Authentication Flow

```
User → Frontend Login Page
  │
  ├─► Click "Sign in with Google"
  │
  ▼
Backend OAuth2 Endpoint (/oauth2/authorization/google)
  │
  ├─► Redirect to Google
  │
  ├─► User authenticates with Google
  │
  ├─► Google redirects back with code
  │
  ├─► Backend exchanges code for user info
  │
  ├─► Create/Update User in database
  │
  ├─► Generate JWT token
  │
  └─► Redirect to frontend with token
      │
      ▼
Frontend stores token in localStorage
  │
  ├─► Token included in all API requests
  │
  └─► Backend validates token on each request
```

## Scalability Considerations

### Current Architecture
- Single-instance deployment
- In-memory agent cache per session
- Direct database connections

### Future Enhancements
- **Horizontal Scaling**: Stateless agents, shared cache (Redis)
- **Message Queue**: RabbitMQ/Kafka for async agent communication
- **Caching**: Redis for market data and agent responses
- **Load Balancing**: Multiple backend instances
- **Database**: Read replicas for scaling reads

## Performance Optimization

1. **Agent Caching**: Per-session agent instances cached
2. **Market Data Caching**: (Future) Cache API responses
3. **Database Indexing**: On userId, symbol fields
4. **Connection Pooling**: HikariCP for database
5. **Async Processing**: (Future) Async agent execution

## Deployment Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Railway / Cloud                  │
│                                                       │
│  ┌──────────────┐         ┌──────────────┐        │
│  │   Frontend   │         │   Backend    │        │
│  │  (React)     │◄───────►│  (Spring)    │        │
│  │  + Auth      │  JWT    │  + OAuth2    │        │
│  └──────────────┘         └──────┬───────┘        │
│                                   │                 │
│                          ┌────────▼────────┐       │
│                          │   PostgreSQL     │       │
│                          │   (Managed)     │       │
│                          └──────────────────┘       │
│                                                       │
│  ┌──────────────┐         ┌──────────────┐         │
│  │    Google    │         │    OpenAI      │         │
│  │   OAuth2     │         │     API      │         │
│  │              │         │  (LLM)       │         │
│  └──────────────┘         └──────────────┘         │
└─────────────────────────────────────────────────────┘
```

## Error Handling

```
┌─────────────────────────────────────────────────────┐
│              Error Handling Flow                     │
│                                                       │
│  Agent Error                                         │
│    │                                                 │
│    ├─► Log error                                     │
│    │                                                 │
│    ├─► Return error JSON                             │
│    │                                                 │
│    └─► WebSocket error message                      │
│                                                       │
│  API Error                                           │
│    │                                                 │
│    ├─► Retry logic (MarketDataService)              │
│    │                                                 │
│    └─► Fallback to cached data                      │
│                                                       │
│  Database Error                                      │
│    │                                                 │
│    └─► Transaction rollback                         │
└─────────────────────────────────────────────────────┘
```

## Monitoring & Observability

### Current
- Logging via SLF4J/Logback
- Spring Boot Actuator (can be enabled)

### Recommended
- Application Performance Monitoring (APM)
- Distributed tracing
- Metrics collection (Prometheus)
- Health checks
- Agent execution metrics

---

For API documentation, see [API.md](./API.md).
For deployment instructions, see [DEPLOYMENT.md](./DEPLOYMENT.md).

