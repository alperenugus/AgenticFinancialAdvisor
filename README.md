# Agentic Financial Advisor

A multi-agent financial advisory system built with 100% open-source LLMs (Ollama) that provides stock market investment recommendations through coordinated specialized AI agents.

## 🎯 Overview

This system uses a multi-agent architecture where specialized AI agents work together to provide comprehensive investment recommendations. Each agent has specific expertise and tools, coordinated by an Orchestrator Agent that manages the workflow.

## ✨ Features

- **Multi-Agent Architecture**: 6 specialized AI agents working in coordination
- **Real-time Market Data**: Integration with Alpha Vantage API for live stock data
- **Risk Assessment**: Automated risk evaluation based on user preferences
- **Portfolio Management**: Track and manage investment portfolios
- **WebSocket Support**: Real-time updates on agent thinking and recommendations
- **100% Open-Source LLM**: Uses Ollama Cloud API (free!)

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │   Chat UI    │  │  Portfolio   │  │ User Profile │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
│         │                  │                  │                │
│         └──────────────────┼──────────────────┘                │
│                            │                                    │
│                    WebSocket / REST API                         │
└────────────────────────────┼────────────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────────────┐
│                    Backend (Spring Boot)                        │
│                            │                                    │
│         ┌──────────────────┴──────────────────┐               │
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
│         │  - User Profiles                          │        │
│         │  - Portfolios                            │        │
│         │  - Recommendations                       │        │
│         └───────────────────────────────────────────┘        │
│                                                               │
│         ┌───────────────────────────────────────────┐        │
│         │      LangChain4j + Ollama                  │        │
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
- Ollama (for local LLM) - [Installation Guide](https://ollama.ai)

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

3. **Set up Ollama (Local Development)**
   ```bash
   # Install Ollama (macOS/Linux)
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Pull the model
   ollama pull llama3.1
   
   # Start Ollama server
   ollama serve
   ```
   
   **For Production**: Deploy Ollama on Railway - it's completely free!
   See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed instructions.

4. **Configure environment variables**
   ```bash
   cd backend
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```
   
   Edit `application.yml` with your settings:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/financialadvisor
       username: postgres
       password: postgres
   
   langchain4j:
     ollama:
       base-url: http://localhost:11434  # Local: http://localhost:11434
                                         # Railway: https://your-ollama.railway.app
       model: llama3.1
   
   market-data:
     alpha-vantage:
       api-key: YOUR_API_KEY  # Get from https://www.alphavantage.co/support/#api-key
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
- **[Deployment Guide](./DEPLOYMENT.md)** - General deployment instructions
- **[Railway Deployment](./docs/railway/RAILWAY_GUIDE.md)** - Complete Railway deployment guide
- **[Environment Variables](./docs/ENVIRONMENT_VARIABLES.md)** - All required environment variables
- **[Troubleshooting](./docs/troubleshooting/)** - Common issues and solutions
- **[Local Development](./docs/RUN_LOCALLY.md)** - Running the app locally

## 🤖 Agents

### 1. Orchestrator Agent
Coordinates all other agents, manages workflows, and synthesizes final responses.

### 2. User Profile Agent
Manages user investment preferences, risk tolerance, goals, and constraints.

**Tools:**
- `getUserProfile(userId)` - Get user profile
- `updateRiskTolerance(userId, level)` - Update risk tolerance
- `getInvestmentGoals(userId)` - Get investment goals

### 3. Market Analysis Agent
Analyzes market data, price trends, and technical indicators.

**Tools:**
- `getStockPrice(symbol, timeframe)` - Get stock price data
- `getMarketNews(symbol, dateRange)` - Get relevant news
- `analyzeTrends(symbol, timeframe)` - Analyze price trends
- `getTechnicalIndicators(symbol)` - Calculate technical indicators

### 4. Risk Assessment Agent
Evaluates risk levels and compares with user tolerance.

**Tools:**
- `assessStockRisk(symbol, metrics)` - Assess individual stock risk
- `calculatePortfolioRisk(portfolio)` - Calculate portfolio risk
- `checkRiskTolerance(stockRisk, userTolerance)` - Compare with user tolerance
- `getRiskMetrics(symbol)` - Get risk metrics (volatility, beta)

### 5. Research Agent
Performs fundamental analysis and company research.

**Tools:**
- `getCompanyFundamentals(symbol)` - Get financial ratios
- `analyzeFinancials(symbol)` - Analyze financial statements
- `compareCompanies(symbols)` - Compare companies
- `getSectorAnalysis(sector)` - Sector analysis

### 6. Recommendation Agent
Synthesizes all information into actionable recommendations.

**Tools:**
- `generateRecommendation(...)` - Generate final recommendation
- `explainReasoning(components)` - Explain recommendation reasoning
- `calculateConfidence(factors)` - Calculate confidence score
- `formatRecommendation(recommendation)` - Format for user

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
- **LangChain4j 0.34.0** - LLM integration
- **Ollama** - Open-source LLM (local development)
- **Ollama** - Free open-source LLM (deploy on Railway)
- **PostgreSQL** - Database
- **WebSocket** - Real-time communication

### Frontend
- **React + Vite** - UI framework
- **Tailwind CSS** - Styling
- **Recharts** - Data visualization
- **WebSocket Client** - Real-time updates

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

- Input validation on all endpoints
- SQL injection prevention (JPA)
- CORS configuration
- Session management
- Rate limiting (recommended for production)

## 🚢 Deployment

### Railway Deployment

1. **Backend Deployment**
   - Connect GitHub repository
   - Set environment variables:
     - `DATABASE_URL`
     - `LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app`
     - `LANGCHAIN4J_OLLAMA_MODEL=llama3.1`
     - `ALPHA_VANTAGE_API_KEY`

2. **Frontend Deployment**
   - Build: `npm run build`
   - Deploy static files to Railway or similar

3. **Database**
   - Use Railway PostgreSQL addon

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed deployment instructions.

## 📊 Example Usage

### Creating a User Profile

```bash
curl -X POST http://localhost:8080/api/profile \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
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
  -d '{
    "userId": "user123",
    "query": "Should I buy Apple stock?",
    "sessionId": "session-123"
  }'
```

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
- Ollama for open-source LLM hosting
- Alpha Vantage for market data API
- Spring Boot community

## 📞 Support

For issues and questions:
- Open an issue on GitHub
- Check [ARCHITECTURE.md](./ARCHITECTURE.md) for system design details
- Check [API.md](./API.md) for API documentation

---

**Built with ❤️ using 100% open-source LLMs**

