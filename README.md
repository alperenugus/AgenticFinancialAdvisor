# Agentic Financial Advisor

A multi-agent financial advisory system powered by Groq API that provides personalized financial advice through coordinated specialized AI agents with full access to user portfolio and profile data.

## 🎯 Overview

This system uses a **multi-agent architecture with individual LLM instances** where specialized AI agents work together to provide comprehensive financial advice. Each agent has its own LLM instance for independent reasoning, coordinated by an Orchestrator Agent that delegates queries and synthesizes responses. The AI Advisor has full access to user portfolio and profile data for personalized recommendations.

## ✨ Features

- **Google Sign-In Authentication**: Secure OAuth2 authentication with JWT tokens
- **Multi-Agent Architecture**: 6 specialized AI agents working in coordination
- **Intelligent Greeting Handling**: Natural conversation flow with contextual financial guidance
- **Portfolio & Profile Access**: AI Advisor has full access to user portfolio and profile data for personalized advice
- **Real-time Market Data**: Integration with Alpha Vantage API for live stock data
- **Risk Assessment**: Automated risk evaluation based on user preferences
- **Portfolio Management**: Track and manage investment portfolios with automatic price updates
- **WebSocket Support**: Real-time updates on agent thinking and analysis
- **Fast LLM Inference**: Powered by Groq API with llama-3.3-70b-versatile (orchestrator) and llama-3.1-8b-instant (tool agents)
- **Professional UI**: Modern, responsive light theme design with stock price graph branding

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  Login Page  │  │   Chat UI    │  │  Portfolio   │        │
│  │ (Google OAuth)│  └──────┬───────┘  └──────┬───────┘        │
│  └──────┬───────┘           │                  │                │
│         │                   │                  │                │
│         └───────────────────┼──────────────────┘                │
│                             │                                    │
│                    WebSocket / REST API (JWT Auth)              │
└────────────────────────────┼────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                    Backend (Spring Boot)                        │
│                            │                                    │
│         ┌──────────────────┴──────────────────┐               │
│         │   Security Layer (OAuth2 + JWT)      │               │
│         │  - Google OAuth2 Authentication     │               │
│         │  - JWT Token Validation            │               │
│         └───────┬──────────────────────────────┘               │
│                 │                                               │
│         ┌───────▼──────────────────────────────┐               │
│         │     Orchestrator Service             │               │
│         │  (Coordinates all agents via LLM)    │               │
│         └───────┬──────────────────────────────┘               │
│                 │                                               │
│    ┌────────────┼────────────┬──────────────┬─────────────┐   │
│    │            │            │              │             │   │
│ ┌──▼──┐    ┌────▼────┐  ┌────▼────┐  ┌────▼────┐  ┌────▼──┐ │
│ │User │    │ Market  │  │  Risk   │  │Research │  │Recomm.│ │
│ │Prof.│    │Analysis │  │Assessment│  │  Agent  │  │ Agent │ │
│ │Agent│    │  Agent  │  │  Agent  │  │         │  │       │ │
│ └──┬──┘    └────┬────┘  └────┬────┘  └────┬────┘  └───┬───┘ │
│    │            │            │              │            │     │
│    └────────────┼────────────┼──────────────┼──────────┘     │
│                 │            │              │                  │
│         ┌───────▼────────────▼──────────────▼───────┐        │
│         │      MarketDataService                     │        │
│         │  (Alpha Vantage API Integration)          │        │
│         └───────────────────────────────────────────┘        │
│                                                               │
│         ┌───────────────────────────────────────────┐        │
│         │      PostgreSQL Database                  │        │
│         │  - Users (Google OAuth)                  │        │
│         │  - User Profiles                          │        │
│         │  - Portfolios                            │        │
│         │  - Recommendations                       │        │
│         └───────────────────────────────────────────┘        │
│                                                               │
│         ┌───────────────────────────────────────────┐        │
│         │      LangChain4j + Groq API                │        │
│         │  (LLM for Agent Coordination)            │        │
│         └───────────────────────────────────────────┘        │
└───────────────────────────────────────────────────────────────┘
```

For detailed architecture documentation, see [ARCHITECTURE.md](./ARCHITECTURE.md).

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 14+ (or use H2 for testing)
- Node.js 18+ and npm (for frontend)
- Groq API Key - [Get one here](https://console.groq.com/)
- Google OAuth2 Credentials - [Setup Guide](./docs/GOOGLE_AUTH_SETUP.md)

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AgenticFinancialAdvisor
   ```

2. **Set up PostgreSQL**
   ```bash
   # Using Docker
   docker run -d --name postgres-financial \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=financialadvisor \
     -p 5432:5432 \
     postgres:14
   ```

3. **Get API Keys**
   - **Groq API Key**: Sign up at [Groq Console](https://console.groq.com/) and create an API key
   - **Alpha Vantage API Key**: Get free key from [Alpha Vantage](https://www.alphavantage.co/support/#api-key)
   - **Google OAuth2 Credentials**: Follow [Google Auth Setup Guide](./docs/GOOGLE_AUTH_SETUP.md)

4. **Configure environment variables**
   
   Set environment variables:
   ```bash
   export GROQ_API_KEY=your_groq_api_key_here
   export ALPHA_VANTAGE_API_KEY=your_alpha_vantage_api_key
   export GOOGLE_CLIENT_ID=your_google_client_id
   export GOOGLE_CLIENT_SECRET=your_google_client_secret
   export JWT_SECRET=your-secure-random-secret-key-minimum-32-characters
   ```
   
   Or edit `application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/financialadvisor
       username: postgres
       password: postgres
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID:}
               client-secret: ${GOOGLE_CLIENT_SECRET:}
   
   langchain4j:
     groq:
       orchestrator:
         api-key: ${GROQ_API_KEY:}
      model: llama-3.3-70b-versatile  # Most capable model for comprehensive analysis
      timeout-seconds: 90  # Increased timeout for complex analysis
      tool-agent:
        api-key: ${GROQ_API_KEY:}
        model: llama-3.1-8b-instant  # Fast, cost-effective for tool calls
   
   jwt:
     secret: ${JWT_SECRET:your-secure-secret-key}
     expiration: 86400000
   
   market-data:
     alpha-vantage:
       api-key: ${ALPHA_VANTAGE_API_KEY:demo}
   ```

5. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

   The backend will start on `http://localhost:8080`

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

## 📚 Documentation

- **[API Documentation](./API.md)** - Complete API reference
- **[Architecture](./ARCHITECTURE.md)** - System architecture and design
- **[Google Auth Setup](./docs/GOOGLE_AUTH_SETUP.md)** - Google Sign-In configuration guide
- **[Deployment Guide](./DEPLOYMENT.md)** - General deployment instructions
- **[Railway Deployment](./docs/railway/RAILWAY_GUIDE.md)** - Complete Railway deployment guide
- **[Environment Variables](./docs/ENVIRONMENT_VARIABLES.md)** - All required environment variables
- **[Troubleshooting](./docs/troubleshooting/)** - Common issues and solutions
- **[Local Development](./docs/RUN_LOCALLY.md)** - Running the app locally

## 🤖 Agents

### 1. Orchestrator Agent
Coordinates all other agent LLMs using its own LLM instance. Delegates queries to specialized agent LLMs and synthesizes their responses into final recommendations.

### 2. User Profile Agent
Has its own LLM instance for reasoning about user profiles and portfolios. Manages user investment preferences, risk tolerance, goals, and constraints. **Also provides portfolio access tools.**

**Tools:**
- `getUserProfile(userId)` - Get user profile (risk tolerance, goals, preferences)
- `updateRiskTolerance(userId, level)` - Update risk tolerance
- `getInvestmentGoals(userId)` - Get investment goals
- `getPortfolio(userId)` - Get complete portfolio with all holdings and current prices
- `getPortfolioHoldings(userId)` - Get list of stocks user owns
- `getPortfolioSummary(userId)` - Get portfolio summary (total value, gain/loss, holdings count)

### 3. Market Analysis Agent
Has its own LLM instance for reasoning about market data. Analyzes stock prices, price trends, and technical indicators.

**Tools:**
- `getStockPrice(symbol, timeframe)` - Get stock price data
- `getMarketNews(symbol, dateRange)` - Get relevant news
- `analyzeTrends(symbol, timeframe)` - Analyze price trends
- `getTechnicalIndicators(symbol)` - Calculate technical indicators

### 4. Web Search Agent
Has its own LLM instance for reasoning about web search queries. Searches the web for financial news, analysis, and market insights using Tavily MCP.

**Tools:**
- `searchFinancialNews(query)` - Search for financial news and market insights
- `searchStockAnalysis(symbol)` - Search for stock analysis and research reports
- `searchMarketTrends(query)` - Search for market trends and sector analysis
- `searchCompanyInfo(symbol)` - Search for company information and earnings

### 5. Fintwit Analysis Agent
Has its own LLM instance for reasoning about social sentiment. Analyzes social sentiment from financial Twitter (fintwit) for market insights.

**Tools:**
- `analyzeFintwitSentiment(symbol)` - Analyze social sentiment for a stock
- `getFintwitTrends(query)` - Get trending topics from financial Twitter
- `searchFintwitContent(query)` - Search for specific content on financial Twitter
- `calculateConfidence(factors)` - Calculate confidence score
- `formatRecommendation(recommendation)` - Format for user

### 7. Stock Discovery Agent
Discovers stocks in real-time based on user criteria. **No hardcoded stock lists** - uses live market data validation.

**Tools:**
- `discoverStocks(riskTolerance, sectors, excludeOwned)` - Find stocks matching criteria with real-time validation
- `validateStockSymbol(symbol)` - Verify if a stock symbol exists and is tradeable

### 8. Portfolio Recommendation Service
Generates portfolio-level recommendations using AI agents and real-time stock discovery.

**Features:**
- **Real-time stock discovery** - No hardcoded lists, validates stocks using live market data
- **Portfolio-focused** - Considers current holdings, diversification, and risk alignment
- **AI-powered** - Uses orchestrator to analyze stocks in context of user's portfolio
- **Automatic generation** - Triggers when profile/portfolio changes

**Triggers:**
- When user profile is created/updated
- When portfolio holdings are added/removed
- When user requests recommendations and none exist
- Manual trigger via `POST /api/advisor/generate-recommendations`

## 🧪 Testing

```bash
# Run all tests
cd backend
mvn test

# Run specific test class
mvn test -Dtest=UserProfileAgentTest

# Run with coverage
mvn test jacoco:report
```

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.4** - Application framework
- **Spring Security OAuth2** - Google Sign-In authentication
- **JWT** - Token-based authentication
- **LangChain4j 0.34.0** - LLM integration
- **Groq API** - Fast LLM inference (llama-3.3-70b for orchestrator and all agent LLMs)
- **PostgreSQL** - Database
- **WebSocket** - Real-time communication

### Frontend
- **React + Vite** - UI framework
- **Tailwind CSS** - Professional light theme styling with financial advisor theme
- **Recharts** - Data visualization (portfolio charts, allocation graphs)
- **WebSocket Client** - Real-time updates
- **Google OAuth Client Library** - Authentication
- **Stock Price Graph Favicon** - Professional branding

### External APIs
- **Alpha Vantage** - Market data
- **Yahoo Finance** - Additional market data
- **NewsAPI** - Financial news

## 📝 Project Structure

```
AgenticFinancialAdvisor/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/agent/financialadvisor/
│   │   │   │   ├── config/          # Configuration classes
│   │   │   │   ├── controller/       # REST controllers
│   │   │   │   ├── model/            # Entity models
│   │   │   │   ├── repository/       # Data repositories
│   │   │   │   ├── service/
│   │   │   │   │   ├── agents/       # AI agents
│   │   │   │   │   └── orchestrator/ # Orchestrator service
│   │   │   │   └── util/             # Utility classes
│   │   │   └── resources/
│   │   │       └── application.yml   # Configuration
│   │   └── test/                      # Test classes
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/                # React components
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── package.json
├── ARCHITECTURE.md                    # Architecture documentation
├── API.md                             # API documentation
├── PROJECT_PLAN.md                    # Project plan
└── README.md                          # This file
```

## 🔒 Security & Disclaimers

### Required Disclaimers

⚠️ **IMPORTANT**: This is a demonstration system for educational purposes only.

- Not a licensed financial advisor
- Consult a professional for actual investment decisions
- Past performance does not guarantee future results
- Investments carry risk of loss

### Security Measures

- **Google OAuth2 Authentication** - Secure passwordless authentication
- **JWT Tokens** - Stateless, secure session management
- **Input validation** on all endpoints
- **SQL injection prevention** (JPA)
- **CORS configuration** with credentials support
- **Token-based authorization** - All endpoints protected
- **Rate limiting** (recommended for production)

## 🚢 Deployment

### Railway Deployment

1. **Backend Deployment**
   - Connect GitHub repository
   - Set environment variables:
     - `DATABASE_URL` (auto-set by Railway PostgreSQL)
     - `GROQ_API_KEY` (required - get from https://console.groq.com/)
     - `ALPHA_VANTAGE_API_KEY` (required)
     - `GOOGLE_CLIENT_ID` (required - see [Google Auth Setup](./docs/GOOGLE_AUTH_SETUP.md))
     - `GOOGLE_CLIENT_SECRET` (required)
     - `JWT_SECRET` (required - generate secure random 32+ character string)
     - `CORS_ORIGINS` (include your frontend URL)
     - Optional: `GROQ_ORCHESTRATOR_MODEL`, `GROQ_TOOL_AGENT_MODEL` (defaults provided)

2. **Frontend Deployment**
   - Build: `npm run build`
   - Deploy static files to Railway or similar

3. **Database**
   - Use Railway PostgreSQL addon

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed deployment instructions.

## 📊 Example Usage

### Authentication

1. **Sign in with Google** (via frontend)
   - Navigate to the application
   - Click "Sign in with Google"
   - Complete OAuth2 flow
   - JWT token is automatically stored

2. **Using API with JWT Token**
   ```bash
   # Get your token from browser localStorage after login
   # Or use /api/auth/me endpoint
   
   TOKEN="your_jwt_token_here"
   
   # Get current user
   curl -X GET http://localhost:8080/api/auth/me \
     -H "Authorization: Bearer $TOKEN"
   ```

### Creating a User Profile

```bash
curl -X POST http://localhost:8080/api/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "riskTolerance": "MODERATE",
    "horizon": "MEDIUM",
    "goals": ["GROWTH", "RETIREMENT"],
    "budget": 10000
  }'
```

### Requesting Investment Analysis

```bash
curl -X POST http://localhost:8080/api/advisor/analyze \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "query": "Should I buy Apple stock?",
    "sessionId": "session-123"
  }'
```

**Note**: All API endpoints now require authentication. The `userId` is automatically extracted from the JWT token.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- LangChain4j for excellent LLM integration
- Groq for fast, cost-effective LLM inference
- Alpha Vantage for market data API
- Spring Boot community

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Check [ARCHITECTURE.md](./ARCHITECTURE.md) for system design details
- Check [API.md](./API.md) for API documentation

---

**Built with ❤️ using Groq API for fast LLM inference**

