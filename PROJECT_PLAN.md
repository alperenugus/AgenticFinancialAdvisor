# Agentic Financial Advisor - Project Plan

## Project Overview

A multi-agent financial advisory system built with 100% open-source LLMs (Ollama) that provides stock market investment recommendations through coordinated specialized agents.

## Architecture

### Agents (6 Total)

1. **Orchestrator Agent** - Coordinates all other agents, manages workflows
2. **User Profile Agent** - Manages user preferences, risk tolerance, goals
3. **Market Analysis Agent** - Analyzes market data, trends, news
4. **Risk Assessment Agent** - Evaluates risk levels, portfolio risk
5. **Research Agent** - Fundamental analysis, company research
6. **Recommendation Agent** - Synthesizes all information into recommendations

### Tech Stack

**Backend:**
- Spring Boot 3.4
- LangChain4j (with Ollama integration)
- PostgreSQL (user data, portfolios, recommendations)
- Redis (optional - for caching market data)
- External APIs: Alpha Vantage (free tier), Yahoo Finance, NewsAPI

**Frontend:**
- React + Vite
- Tailwind CSS
- Recharts (for portfolio visualization)
- WebSocket (real-time agent thinking updates)

**LLM:**
- Ollama (local development)
- Model: llama3.1 or mistral (supports structured output)

**Deployment:**
- Railway (backend + frontend)
- Railway PostgreSQL
- Note: Deploy Ollama on Railway - completely free and open-source!

## Project Structure

```
AgenticFinancialAdvisor/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agent/financialadvisor/
│   │   │   │   ├── FinancialAdvisorApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── LangChain4jConfig.java
│   │   │   │   │   ├── WebConfig.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AdvisorController.java
│   │   │   │   │   ├── PortfolioController.java
│   │   │   │   │   └── UserProfileController.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── UserProfile.java
│   │   │   │   │   ├── Portfolio.java
│   │   │   │   │   ├── StockHolding.java
│   │   │   │   │   ├── Recommendation.java
│   │   │   │   │   ├── AgentResponse.java
│   │   │   │   │   └── ConversationContext.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserProfileRepository.java
│   │   │   │   │   ├── PortfolioRepository.java
│   │   │   │   │   └── RecommendationRepository.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── orchestrator/
│   │   │   │   │   │   └── OrchestratorService.java
│   │   │   │   │   ├── agents/
│   │   │   │   │   │   ├── UserProfileAgent.java
│   │   │   │   │   │   ├── MarketAnalysisAgent.java
│   │   │   │   │   │   ├── RiskAssessmentAgent.java
│   │   │   │   │   │   ├── ResearchAgent.java
│   │   │   │   │   │   └── RecommendationAgent.java
│   │   │   │   │   ├── MarketDataService.java
│   │   │   │   │   ├── PortfolioService.java
│   │   │   │   │   └── WebSocketService.java
│   │   │   │   └── util/
│   │   │   │       ├── ReActParser.java
│   │   │   │       └── DateParser.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   ├── pom.xml
│   └── docker-compose.yml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── ChatComponent.jsx
│   │   │   ├── PortfolioView.jsx
│   │   │   ├── RecommendationCard.jsx
│   │   │   └── UserProfileForm.jsx
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── index.css
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
├── README.md
├── PROJECT_PLAN.md
└── .gitignore
```

## Agent Responsibilities

### 1. Orchestrator Agent
- Receives user requests
- Coordinates workflow between agents
- Manages agent communication
- Synthesizes final responses
- Handles error recovery

**Tools:**
- `coordinateAnalysis(userId, symbol, requestType)` - Orchestrates analysis workflow
- `getAgentStatus()` - Check agent availability
- `resolveConflict(conflictingRecommendations)` - Resolve agent conflicts

### 2. User Profile Agent
- Manages user investment profile
- Tracks risk tolerance
- Stores investment goals
- Manages constraints and preferences

**Tools:**
- `getUserProfile(userId)` - Get user profile
- `updateRiskTolerance(userId, level)` - Update risk tolerance
- `getInvestmentGoals(userId)` - Get user goals
- `updatePreferences(userId, preferences)` - Update preferences

### 3. Market Analysis Agent
- Fetches real-time stock data
- Analyzes price trends
- Monitors market news
- Calculates technical indicators

**Tools:**
- `getStockPrice(symbol, timeframe)` - Get stock price data
- `getMarketNews(symbol, dateRange)` - Get relevant news
- `analyzeTrends(symbol, timeframe)` - Analyze price trends
- `getTechnicalIndicators(symbol)` - Calculate technical indicators

### 4. Risk Assessment Agent
- Evaluates stock risk levels
- Calculates portfolio risk
- Assesses risk vs. user tolerance
- Provides risk warnings

**Tools:**
- `assessStockRisk(symbol, metrics)` - Assess individual stock risk
- `calculatePortfolioRisk(portfolio)` - Calculate portfolio risk
- `checkRiskTolerance(stockRisk, userTolerance)` - Compare with user tolerance
- `getRiskMetrics(symbol)` - Get risk metrics (volatility, beta)

### 5. Research Agent
- Analyzes company fundamentals
- Reads financial reports
- Compares companies
- Provides investment thesis

**Tools:**
- `getCompanyFundamentals(symbol)` - Get financial ratios
- `analyzeFinancials(symbol)` - Analyze financial statements
- `compareCompanies(symbols)` - Compare companies
- `getSectorAnalysis(sector)` - Sector analysis

### 6. Recommendation Agent
- Synthesizes all agent inputs
- Generates recommendations
- Explains reasoning
- Provides confidence scores

**Tools:**
- `generateRecommendation(analysis, risk, research, userProfile)` - Generate recommendation
- `explainReasoning(components)` - Explain recommendation reasoning
- `calculateConfidence(factors)` - Calculate confidence score
- `formatRecommendation(recommendation)` - Format for user

## Data Models

### UserProfile
```java
@Entity
public class UserProfile {
    private Long id;
    private String userId; // session-based or user ID
    private RiskTolerance riskTolerance; // CONSERVATIVE, MODERATE, AGGRESSIVE
    private InvestmentHorizon horizon; // SHORT, MEDIUM, LONG
    private List<String> goals; // RETIREMENT, GROWTH, INCOME
    private BigDecimal budget;
    private List<String> preferredSectors;
    private List<String> excludedSectors;
    private boolean ethicalInvesting; // ESG preferences
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Portfolio
```java
@Entity
public class Portfolio {
    private Long id;
    private String userId;
    private List<StockHolding> holdings;
    private BigDecimal totalValue;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercent;
    private LocalDateTime lastUpdated;
}

@Entity
public class StockHolding {
    private Long id;
    private String symbol;
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal value;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPercent;
}
```

### Recommendation
```java
@Entity
public class Recommendation {
    private Long id;
    private String userId;
    private String symbol;
    private RecommendationAction action; // BUY, SELL, HOLD
    private Double confidence; // 0.0 - 1.0
    private String reasoning;
    private RiskLevel riskLevel; // LOW, MEDIUM, HIGH
    private BigDecimal targetPrice;
    private InvestmentHorizon timeHorizon;
    private String marketAnalysis;
    private String riskAssessment;
    private String researchSummary;
    private LocalDateTime createdAt;
}
```

## Implementation Phases

### Phase 1: Project Setup & Core Infrastructure
- [ ] Initialize Spring Boot project
- [ ] Set up PostgreSQL schema
- [ ] Configure LangChain4j with Ollama
- [ ] Set up WebSocket for real-time updates
- [ ] Create basic frontend structure
- [ ] Set up Railway deployment config

### Phase 2: User Profile Agent
- [ ] Create UserProfile entity and repository
- [ ] Implement UserProfileAgent with tools
- [ ] Create REST endpoints for profile management
- [ ] Build frontend profile form

### Phase 3: Market Data Integration
- [ ] Integrate Alpha Vantage API
- [ ] Create MarketDataService
- [ ] Implement Market Analysis Agent
- [ ] Add caching for market data

### Phase 4: Risk Assessment Agent
- [ ] Implement risk calculation logic
- [ ] Create Risk Assessment Agent
- [ ] Add risk metrics calculation
- [ ] Integrate with portfolio data

### Phase 5: Research Agent
- [ ] Integrate financial data APIs
- [ ] Implement fundamental analysis
- [ ] Create Research Agent
- [ ] Add company comparison tools

### Phase 6: Recommendation Agent
- [ ] Implement recommendation synthesis logic
- [ ] Create Recommendation Agent
- [ ] Add confidence scoring
- [ ] Format recommendations for display

### Phase 7: Orchestrator Agent
- [ ] Implement workflow orchestration
- [ ] Create Orchestrator Agent
- [ ] Add agent coordination logic
- [ ] Implement conflict resolution

### Phase 8: Frontend & UI
- [ ] Build chat interface for recommendations
- [ ] Create portfolio visualization
- [ ] Add recommendation cards
- [ ] Implement real-time updates
- [ ] Add charts and graphs

### Phase 9: Testing & Polish
- [ ] Write unit tests
- [ ] Integration testing
- [ ] UI/UX improvements
- [ ] Error handling
- [ ] Documentation

## API Endpoints

### User Profile
- `GET /api/profile/{userId}` - Get user profile
- `POST /api/profile` - Create user profile
- `PUT /api/profile/{userId}` - Update user profile

### Portfolio
- `GET /api/portfolio/{userId}` - Get user portfolio
- `POST /api/portfolio/{userId}/holdings` - Add holding
- `DELETE /api/portfolio/{userId}/holdings/{holdingId}` - Remove holding

### Recommendations
- `POST /api/advisor/analyze` - Request analysis (orchestrator)
- `GET /api/advisor/recommendations/{userId}` - Get recommendations
- `GET /api/advisor/recommendations/{userId}/{symbol}` - Get specific recommendation

### WebSocket
- `/ws` - WebSocket endpoint for real-time updates
- `/topic/thinking/{sessionId}` - Thinking steps
- `/topic/response/{sessionId}` - Final responses

## External API Integration

### Alpha Vantage (Free Tier)
- 5 API calls per minute
- 500 calls per day
- Endpoints: TIME_SERIES_DAILY, COMPANY_OVERVIEW, NEWS_SENTIMENT

### Yahoo Finance (Unofficial)
- No API key required
- Rate limits: ~2000 requests/hour
- Use yfinance library or REST API

### NewsAPI
- Free tier: 100 requests/day
- Financial news endpoint

## Security & Disclaimers

### Required Disclaimers
- "This is a demonstration system for educational purposes only."
- "Not a licensed financial advisor. Consult a professional for actual investment decisions."
- "Past performance does not guarantee future results."
- "Investments carry risk of loss."

### Security Measures
- Input validation
- Rate limiting
- Session management
- Data encryption
- SQL injection prevention

## Deployment Strategy

### Railway Configuration
- Backend service with PostgreSQL
- Frontend service (static build)
- Environment variables for API keys
- LLM provider configuration (Ollama deployed on Railway - free!)

### Environment Variables
```
# Database
DATABASE_URL=postgresql://...
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...

# LLM (Ollama deployed on Railway - FREE!)
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Market Data APIs
ALPHA_VANTAGE_API_KEY=...
NEWS_API_KEY=...
```

## Success Criteria

- [ ] All 6 agents working and coordinating
- [ ] User can get investment recommendations
- [ ] Portfolio tracking functional
- [ ] Real-time updates via WebSocket
- [ ] Deployed to Railway
- [ ] User-friendly UI
- [ ] Proper disclaimers in place

## Next Steps

1. Initialize project structure
2. Set up Spring Boot backend
3. Configure LangChain4j with Ollama
4. Create database schema
5. Implement User Profile Agent first (simplest)
6. Then Market Analysis Agent
7. Continue with remaining agents
8. Build frontend
9. Deploy to Railway


