# System Architecture

## Overview

The Agentic Financial Advisor uses a **multi-agent architecture** where specialized AI agents collaborate to provide investment recommendations. The system is built on Spring Boot with LangChain4j for LLM integration, using Ollama (deployed on Railway) - completely free and open-source!

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                    │
│                                                                               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐        │
│  │   React Frontend │  │  Mobile App      │  │  API Clients     │        │
│  │   (Vite + React) │  │  (Future)        │  │  (Third-party)    │        │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘        │
│           │                      │                      │                   │
│           └──────────────────────┼──────────────────────┘                   │
│                                  │                                            │
│                          HTTP/REST + WebSocket                                │
└──────────────────────────────────┼────────────────────────────────────────────┘
                                   │
┌──────────────────────────────────┼────────────────────────────────────────────┐
│                         APPLICATION LAYER                                      │
│                                   │                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        REST Controllers                              │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │   │
│  │  │   Advisor    │  │   Portfolio  │  │ User Profile │             │   │
│  │  │  Controller  │  │  Controller │  │  Controller  │             │   │
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
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │  │
│  │  │   User       │  │  Portfolio   │  │Recommendation│                 │  │
│  │  │  Profile     │  │  Repository  │  │  Repository  │                 │  │
│  │  │  Repository  │  │              │  │              │                 │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                 │  │
│  └─────────┼─────────────────┼─────────────────┼──────────────────────────┘  │
│            │                 │                 │                               │
│  ┌─────────▼─────────────────▼─────────────────▼──────────────────────────┐ │
│  │                    PostgreSQL Database                                   │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                │ │
│  │  │   user_      │  │  portfolios  │  │recommendations│                │ │
│  │  │   profiles   │  │              │  │               │                │ │
│  │  └──────────────┘  └──────────────┘  └───────────────┘                │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL SERVICES                                    │
│                                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │   Ollama     │  │   Alpha      │  │   NewsAPI    │                      │
│  │  (Local LLM) │  │ (Production) │  │  Vantage     │                      │
│  │              │  │              │  │  (Market     │                      │
│  │  llama3.1    │  │  llama-3.1   │  │   Data)      │                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                      │
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
         ├─► Coordinate agent execution:
         │   │
         │   ├─► UserProfileAgent.getUserProfile()
         │   │   └─► Get user risk tolerance, goals
         │   │
         │   ├─► MarketAnalysisAgent.getStockPrice()
         │   │   ├─► MarketAnalysisAgent.analyzeTrends()
         │   │   └─► MarketAnalysisAgent.getMarketNews()
         │   │
         │   ├─► RiskAssessmentAgent.assessStockRisk()
         │   │   └─► RiskAssessmentAgent.checkRiskTolerance()
         │   │
         │   ├─► ResearchAgent.getCompanyFundamentals()
         │   │   └─► ResearchAgent.analyzeFinancials()
         │   │
         │   └─► RecommendationAgent.generateRecommendation()
         │       └─► Synthesize all inputs
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
│                    Orchestrator Agent                        │
│  (LangChain4j AI with access to all agent tools)           │
└───────────────┬─────────────────────────────────────────────┘
                │
                │ Tool Calls (via @Tool annotations)
                │
    ┌───────────┼───────────┬───────────┬───────────┐
    │           │           │           │           │
    ▼           ▼           ▼           ▼           ▼
┌───────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ User  │  │ Market │  │  Risk  │  │Research│  │Recomm.│
│Profile│  │Analysis│  │Assessment│ │ Agent  │  │ Agent │
│ Agent │  │ Agent  │  │  Agent  │  │        │  │       │
└───┬───┘  └───┬────┘  └───┬────┘  └───┬────┘  └───┬───┘
    │           │            │           │           │
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

### 1. Orchestrator Service

**Purpose**: Central coordinator that uses LangChain4j to create an AI agent with access to all specialized agent tools.

**Key Features**:
- Maintains conversation context per session
- Automatically selects which tools to call based on user query
- Synthesizes responses from multiple agents
- Sends real-time updates via WebSocket

**Implementation**:
```java
@Bean
public FinancialAdvisorAgent createAgent() {
    return AiServices.builder(FinancialAdvisorAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .tools(
            userProfileAgent,
            marketAnalysisAgent,
            riskAssessmentAgent,
            researchAgent,
            recommendationAgent
        )
        .build();
}
```

### 2. Agent Tools Pattern

Each agent exposes tools via `@Tool` annotations that the LLM can call:

```java
@Tool("Get user's investment profile...")
public String getUserProfile(String userId) {
    // Implementation
}
```

The LLM automatically:
- Understands when to call each tool
- Passes correct parameters
- Handles tool responses
- Combines multiple tool results

### 3. Data Flow

```
User Request
    │
    ▼
Controller (REST/WebSocket)
    │
    ▼
Orchestrator Service
    │
    ├─► LLM decides which tools to call
    │
    ├─► Tool 1: UserProfileAgent.getUserProfile()
    │   └─► Repository → Database
    │
    ├─► Tool 2: MarketAnalysisAgent.getStockPrice()
    │   └─► MarketDataService → Alpha Vantage API
    │
    ├─► Tool 3: RiskAssessmentAgent.assessStockRisk()
    │   └─► Uses MarketDataService results
    │
    ├─► Tool 4: ResearchAgent.getCompanyFundamentals()
    │   └─► MarketDataService → Alpha Vantage API
    │
    └─► Tool 5: RecommendationAgent.generateRecommendation()
        └─► Synthesizes all previous results
            └─► Repository → Save to Database
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
- **Ollama**: Local open-source LLM (development)
- **Ollama**: Free open-source LLM (deployed on Railway)

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
│  1. Input Validation (Spring Validation)            │
│  2. SQL Injection Prevention (JPA)                  │
│  3. CORS Configuration                              │
│  4. Rate Limiting (Recommended)                     │
│  5. Authentication (Future: OAuth2/JWT)             │
└─────────────────────────────────────────────────────┘
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
│  │  (Static)    │◄───────►│  (Spring)    │        │
│  └──────────────┘         └──────┬───────┘        │
│                                   │                 │
│                          ┌────────▼────────┐       │
│                          │   PostgreSQL     │       │
│                          │   (Managed)     │       │
│                          └──────────────────┘       │
│                                                       │
│                          ┌──────────────┐           │
│                          │   Ollama     │           │
│                          │  (LLM API)   │           │
│                          └──────────────┘           │
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

