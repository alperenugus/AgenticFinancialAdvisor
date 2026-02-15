# System Architecture

## Overview

The Agentic Financial Advisor uses a **multi-agent architecture** where specialized AI agents collaborate to provide investment recommendations. The system is built on Spring Boot with LangChain4j for LLM integration, powered by Groq API for fast LLM inference. The application uses Google OAuth2 for secure authentication with JWT tokens.

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
│  │  │  Spring Security + OAuth2                                    │   │   │
│  │  │  - Google OAuth2 Authentication                              │   │   │
│  │  │  - JWT Token Validation                                      │   │   │
│  │  │  - User Authentication                                       │   │   │
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
│  │                    ORCHESTRATOR SERVICE                               │   │
│  │  ┌──────────────────────────────────────────────────────────────┐   │   │
│  │  │  LangChain4j AI Agent                                        │   │   │
│  │  │  - Coordinates all agents                                    │   │   │
│  │  │  - Manages workflow                                          │   │   │
│  │  │  - Synthesizes responses                                     │   │   │
│  │  └──────────────────────────────────────────────────────────────┘   │   │
│  └───────────┬───────────────────────────────────────────────────────────┘   │
│              │                                                                │
│  ┌───────────┴───────────────────────────────────────────────────────────┐  │
│  │                         AGENT LAYER                                    │  │
│  │                                                                         │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                │  │
│  │  │    User      │  │    Market    │  │     Risk     │                │  │
│  │  │   Profile    │  │   Analysis   │  │ Assessment   │                │  │
│  │  │    Agent     │  │    Agent     │  │    Agent     │                │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                │  │
│  │         │                 │                  │                         │  │
│  │  ┌──────▼─────────────────▼──────────────────▼───────┐              │  │
│  │  │              Research Agent                          │              │  │
│  │  └──────────────────────┬──────────────────────────────┘              │  │
│  │                         │                                               │  │
│  │  ┌──────────────────────▼──────────────────────────────┐              │  │
│  │  │          Recommendation Agent                       │              │  │
│  │  │  (Synthesizes all inputs into final recommendation) │              │  │
│  │  └──────────────────────┬──────────────────────────────┘              │  │
│  │                         │                                               │  │
│  │  ┌──────────────────────▼──────────────────────────────┐              │  │
│  │  │          Stock Discovery Agent                      │              │  │
│  │  │  (Real-time stock discovery, no hardcoded lists)    │              │  │
│  │  └──────────────────────────────────────────────────────┘              │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                   │                                            │
│  ┌─────────────────────────────────▼──────────────────────────────────────┐  │
│  │                      SERVICE LAYER                                      │  │
│  │  ┌──────────────────────────────────────────────────────────────────┐  │  │
│  │  │              MarketDataService                                   │  │  │
│  │  │  - Alpha Vantage API integration                                │  │  │
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
│  │    Google    │  │    Groq      │  │   Alpha     │  │   NewsAPI   │     │
│  │   OAuth2     │  │     API      │  │  Vantage    │  │  (Optional) │     │
│  │              │  │              │  │  (Market     │  │             │     │
│  │ Authentication│  │  LLM Inference│  │   Data)     │  │             │     │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────────────────────────────────────────────────────────┘
```

## Agent Workflow

### Request Flow Diagram

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
│ Orchestrator     │
│   Service        │
│  (LangChain4j)  │
└────────┬─────────┘
         │
         ├─► Parse user query
         │
         ├─► Determine required agents
         │
         ├─► Delegate to agent LLMs:
         │   │
         │   ├─► delegateToUserProfileAgent()
         │   │   └─► UserProfileAgent LLM reasons and calls tools
         │   │       ├─► getUserProfile() → Get user risk tolerance, goals
         │   │       └─► getPortfolioSummary() → Get user's current holdings
         │   │
         │   ├─► delegateToMarketAnalysisAgent()
         │   │   └─► MarketAnalysisAgent LLM reasons and calls tools
         │   │       ├─► getStockPrice()
         │   │       ├─► analyzeTrends()
         │   │       └─► getMarketNews()
         │   │
         │   ├─► delegateToWebSearchAgent()
         │   │   └─► WebSearchAgent LLM reasons and calls tools
         │   │       ├─► searchFinancialNews()
         │   │       └─► searchStockAnalysis()
         │   │
         │   └─► delegateToFintwitAnalysisAgent()
         │       └─► FintwitAnalysisAgent LLM reasons and calls tools
         │           └─► analyzeFintwitSentiment()
         │
         ├─► Synthesize responses from all agent LLMs
         │
         ▼
┌─────────────────┐
│ Final Response  │
│  (JSON)         │
└─────────────────┘
```

### Agent Communication Pattern

```
┌─────────────────────────────────────────────────────────────┐
│              Orchestrator Agent (Orchestrator LLM)          │
│  - Analyzes user query                                      │
│  - Decides which agent(s) to delegate to                    │
│  - Synthesizes responses from multiple agents               │
└───────────────┬─────────────────────────────────────────────┘
                │
                │ Delegation Tool Calls
                │ (delegateToUserProfileAgent, etc.)
                │
    ┌───────────┼───────────┬───────────┬───────────┐
    │           │           │           │           │
    ▼           ▼           ▼           ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ User  │  │ Market │  │   Web  │  │Fintwit │  │Security│
│Profile│  │Analysis│  │ Search │  │Analysis│  │ Agent  │
│ Agent │  │ Agent  │  │ Agent  │  │ Agent  │  │        │
│(LLM)  │  │ (LLM)  │  │ (LLM)  │  │ (LLM)  │  │ (LLM)  │
└───┬───┘  └───┬────┘  └───┬────┘  └───┬────┘  └───┬───┘
    │           │            │           │           │
    │           │            │           │           │
    │           │            │           │           │
    │  Each agent uses its own LLM to reason and call tools  │
    │           │            │           │           │
    └───────────┼────────────┼───────────┼───────────┘
                │            │           │
                ▼            ▼           ▼
         ┌──────────────────────────────────┐
         │    MarketDataService            │
         │  (External API calls)          │
         └──────────────────────────────────┘
                │
                ▼
         ┌──────────────────────────────────┐
         │    PostgreSQL Database           │
         │  (User data, portfolios)         │
         └──────────────────────────────────┘
```

## Component Details

### 0. Stock Discovery Agent

**Purpose**: Discovers stocks in real-time based on user criteria. **No hardcoded stock lists** - validates stocks using live market data.

**Key Features**:
- Real-time stock validation using MarketDataService
- Discovers stocks based on risk tolerance, sectors, and exclusion criteria
- Validates stock symbols exist and are tradeable
- Returns list of valid stocks matching criteria

**Tools**:
- `discoverStocks(riskTolerance, sectors, excludeOwned)` - Find stocks matching criteria
- `validateStockSymbol(symbol)` - Verify stock exists and get current price

**Implementation**:
```java
@Tool("Discover stocks that match specific criteria...")
public String discoverStocks(String riskTolerance, String sectors, String excludeOwned) {
    // Validate stocks from popular list using real-time market data
    // Return only valid, tradeable stocks
}
```

**Note**: Portfolio Recommendation Service is currently disabled. Will be re-enabled in a future iteration.

### 1. Orchestrator Service

**Purpose**: Central coordinator that uses its own LLM instance to coordinate between specialized agent LLMs. Each agent has its own LLM instance for independent reasoning.

**Key Features**:
- Maintains conversation context per session
- Uses delegation tools to route queries to appropriate agent LLMs
- Synthesizes responses from multiple agent LLMs
- Sends real-time updates via WebSocket
- **Intelligent greeting handling** - Responds naturally to greetings and guides users to financial questions
- **Portfolio-aware** - Always checks user portfolio and profile before making recommendations
- **Contextual queries** - Automatically includes user profile and portfolio context in every query

**Architecture**:
- **Orchestrator LLM**: Own LLM instance (llama-3.3-70b-versatile) that coordinates between agents
- **Agent LLMs**: Each agent (UserProfile, MarketAnalysis, WebSearch, Fintwit) has its own LLM instance
- **Delegation Pattern**: Orchestrator uses tools to delegate queries to agent LLMs:
  - `delegateToUserProfileAgent(sessionId, query)`
  - `delegateToMarketAnalysisAgent(sessionId, query)`
  - `delegateToWebSearchAgent(sessionId, query)`
  - `delegateToFintwitAnalysisAgent(sessionId, query)`

**Implementation**:
```java
// Orchestrator has its own LLM
OrchestratorAgent orchestrator = AiServices.builder(OrchestratorAgent.class)
    .chatLanguageModel(chatLanguageModel)  // Orchestrator LLM
    .chatMemoryProvider(memoryProvider)
    .tools(new AgentOrchestrationTools(...))  // Delegation tools
    .build();

// Each agent has its own LLM
UserProfileAgentService agent = AiServices.builder(UserProfileAgentService.class)
    .chatLanguageModel(agentChatLanguageModel)  // Agent LLM
    .chatMemoryProvider(memoryProvider)
    .tools(this)  // Agent's own tools
    .build();
```

### 2. Multi-Agent LLM Architecture

**Key Innovation**: Each agent has its own LLM instance, enabling independent reasoning and specialized behavior.

**Agent LLM Configuration**:
- All agents use the same `agentChatLanguageModel` bean (llama-3.3-70b-versatile)
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
Orchestrator Service (Orchestrator LLM)
    │
    ├─► Orchestrator LLM analyzes query
    │
    ├─► Orchestrator decides which agent(s) to delegate to
    │
    ├─► Delegation 1: delegateToUserProfileAgent()
    │   └─► UserProfileAgent LLM reasons about query
    │       └─► Calls tools: getUserProfile(), getPortfolio()
    │           └─► Repository → Database
    │
    ├─► Delegation 2: delegateToMarketAnalysisAgent()
    │   └─► MarketAnalysisAgent LLM reasons about query
    │       └─► Calls tools: getStockPrice(), analyzeTrends()
    │           └─► MarketDataService → Alpha Vantage API
    │
    ├─► Delegation 3: delegateToWebSearchAgent()
    │   └─► WebSearchAgent LLM reasons about query
    │       └─► Calls tools: searchFinancialNews(), searchStockAnalysis()
    │           └─► Tavily MCP → Web search results
    │
    └─► Delegation 4: delegateToFintwitAnalysisAgent()
        └─► FintwitAnalysisAgent LLM reasons about query
            └─► Calls tools: getFintwitSentiment()
                └─► Twitter API / Web search → Social sentiment
    │
    ├─► Orchestrator LLM synthesizes responses from all agents
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
- **Groq API**: Fast LLM inference
  - **Orchestrator LLM**: llama-3.3-70b-versatile (coordination and synthesis)
  - **Agent LLMs**: llama-3.3-70b-versatile (each agent has its own instance)
  - **Tool Agent LLM**: llama-3.1-8b-instant (available for future optimization)

### Authentication & Security
- **Spring Security OAuth2**: Google Sign-In integration
- **JWT (JSON Web Tokens)**: Stateless authentication
- **OAuth2 Client**: Google OAuth2 flow

### Database
- **PostgreSQL**: Primary database
- **H2**: In-memory database (testing)

### External APIs
- **Alpha Vantage**: Market data (free tier: 5 calls/min, 500/day)
- **Yahoo Finance**: Additional market data (unofficial)
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
│  │    Google    │         │    Groq      │         │
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

