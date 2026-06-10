# System Design V2: Enhanced Agent Architecture

## Overview

This document outlines the improved agent architecture for the Financial Advisor AI system, incorporating web search, fintwit analysis, and enhanced real-time market data capabilities.

## Current Status

✅ **All Issues Resolved**:
1. ✅ **Function Calling**: Fixed - All agents use gpt-4o via OpenAI for reliable tool calling
2. ✅ **Data Sources**: Finnhub API integrated for market data with high rate limits
3. ✅ **Web Search**: WebSearchAgent implemented with Tavily/Serper API support
4. ✅ **Social Sentiment**: FintwitAnalysisAgent implemented with Twitter API and web search fallback
5. ✅ **Agent Coordination**: Plan-Execute-Evaluate architecture with self-correction implemented

## Proposed Agent Architecture

### Core Agents (Plan-Execute-Evaluate Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│         OrchestratorService (Plan-Execute-Evaluate Loop)    │
│                                                             │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────────┐ │
│  │ PlannerAgent │  │ EvaluatorAgent │  │ SecurityAgent  │ │
│  │ (70B LLM)    │  │ (70B LLM)      │  │ (70B LLM)      │ │
│  │ Creates plan │  │ Reviews results│  │ Validates input│ │
│  └──────────────┘  └────────────────┘  └────────────────┘ │
└───────────────┬─────────────────────────────────────────────┘
                │
                │ Plan steps executed in parallel
                │
    ┌───────────┼───────────┬───────────┐
    │           │           │           │
    ▼           ▼           ▼           ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ User   │ │Market │ │  Web   │ │Fintwit │
│Profile │ │Analysis│ │ Search │ │Analysis│
│ Agent  │ │ Agent │ │ Agent  │ │ Agent  │
│ (70B)  │ │ (70B) │ │ (70B)  │ │ (70B)  │
└────────┘ └────────┘ └────────┘ └────────┘
    │           │           │           │
    │  All agents use gpt-4o via OpenAI       │
    └───────────┼───────────┼───────────┘
                │           │
                ▼           ▼
         ┌──────────────────────────────┐
         │    MarketDataService         │
         │  (Finnhub API)              │
         └──────────────────────────────┘
```

### 1. User Profile Agent (Existing - Enhanced)
**Purpose**: Access user portfolio and profile data

**Tools**:
- `getUserProfile(userId)` - User preferences, risk tolerance, goals
- `getPortfolio(userId)` - Complete portfolio with holdings
- `getPortfolioSummary(userId)` - Portfolio overview
- `getPortfolioHoldings(userId)` - List of owned stocks

**Status**: ✅ Already implemented and working

### 2. Market Data Agent (Existing - Enhanced)
**Purpose**: Real-time market data from multiple sources

**Current Tools**:
- `getStockPrice(symbol)` - Current stock price (Finnhub)
- `getStockPriceData(symbol, timeframe)` - Historical price data (Finnhub candles)
- `getMarketNews(symbol)` - Market news (Finnhub company news)
- `analyzeTrends(symbol, timeframe)` - Trend analysis
- `getTechnicalIndicators(symbol)` - Technical indicators

**Status**: ✅ Fully implemented with Finnhub API

### 3. Web Search Agent (NEW)
**Purpose**: Search the web for latest financial news, analysis, and market insights

**Recommended Tool**: **Tavily API** (best for LLM agents)
- Purpose-built for LLM agents
- Returns structured, relevant results
- Good for financial queries
- Free tier: 1,000 searches/month
- Paid: $20/month for 10,000 searches

**Alternative**: **Serper API**
- Google Search API
- Good results but less LLM-optimized
- Free tier: 2,500 searches/month
- Paid: $50/month for 10,000 searches

**Tools to Implement**:
- `searchFinancialNews(query)` - Search for financial news
- `searchStockAnalysis(symbol)` - Search for stock analysis
- `searchMarketTrends(query)` - Search for market trends
- `searchCompanyInfo(symbol)` - Search for company information

**Implementation**: Create `WebSearchAgent.java` using Tavily API

### 4. Fintwit Analysis Agent (NEW)
**Purpose**: Analyze financial Twitter sentiment and trends

**Recommended Approach**: **Twitter API v2** (if available) or **Alternative APIs**

**Options**:
1. **Twitter API v2** (if you have access)
   - Search tweets by keyword/hashtag
   - Get sentiment analysis
   - Track trending financial topics
   - Cost: Free tier limited, paid plans available

2. **Nitter API** (Open source Twitter frontend)
   - No API key needed
   - Can scrape Twitter data
   - Rate limits apply

3. **TwitScraper** or similar tools
   - Open source alternatives
   - May have legal/ToS concerns

4. **Alternative**: Use web search to find fintwit content
   - Search for "fintwit [symbol]" or "[symbol] twitter sentiment"
   - Use Web Search Agent to find relevant tweets/articles

**Tools to Implement**:
- `getFintwitSentiment(symbol)` - Get sentiment from financial Twitter
- `getFintwitTrends(query)` - Get trending financial topics
- `analyzeFintwitMentions(symbol)` - Analyze mentions and sentiment

**Implementation**: Create `FintwitAnalysisAgent.java` using Twitter API or web search fallback

### 5. Risk Assessment Agent (Proposed)
**Purpose**: Evaluate risk levels

**Proposed Tools**:
- `assessStockRisk(symbol, metrics)` - Individual stock risk
- `calculatePortfolioRisk(userId)` - Portfolio risk
- `checkRiskTolerance(userId, symbol)` - Compare with user tolerance
- `getRiskMetrics(symbol)` - Risk metrics (volatility, beta)

**Status**: ⏳ Not yet implemented. Risk considerations are currently handled inline by the orchestrator and existing agents rather than a dedicated Risk Assessment Agent.

### 6. Research Agent (Proposed)
**Purpose**: Fundamental analysis and company research

**Proposed Tools**:
- `getCompanyFundamentals(symbol)` - Financial ratios
- `analyzeFinancials(symbol)` - Financial analysis
- `compareCompanies(symbols)` - Company comparison
- `getSectorAnalysis(symbol)` - Sector analysis

**Possible Enhancements**:
- Integrate with Web Search Agent for latest research
- Add earnings analysis
- Add analyst ratings

**Status**: ⏳ Not yet implemented. Fundamental research needs are currently served by the Web Search Agent.

### 7. Orchestrator Service (Rewritten - Plan-Execute-Evaluate)
**Purpose**: Coordinate all agents via structured agentic loop

**Architecture**:
- **PlannerAgent**: LLM creates structured JSON execution plan from user query
- **Executor**: Runs plan steps in parallel via sub-agents
- **EvaluatorAgent**: Reviews results, synthesizes response, or requests retry
- Self-correcting via retry loop (max 2 retries)
- No hardcoded patterns -- all query understanding done by LLMs

**Status**: ✅ Fully rewritten and working

## Implementation Status

### ✅ Phase 1: Web Search Agent - COMPLETE
- ✅ Tavily API integration implemented
- ✅ `WebSearchAgent.java` created with all tools
- ✅ Integrated with Orchestrator

### ✅ Phase 2: Fintwit Analysis Agent - COMPLETE
- ✅ `FintwitAnalysisAgent.java` created
- ✅ Twitter API support (optional, uses web search fallback)
- ✅ Sentiment analysis tools implemented
- ✅ Integrated with Orchestrator

### ✅ Phase 3: Market Data - COMPLETE
- ✅ Finnhub API fully integrated
- ✅ Real-time data fetching (no cache)
- ✅ Technical indicators implemented

### ✅ Phase 4: Orchestrator - COMPLETE
- ✅ Plan-Execute-Evaluate architecture implemented
- ✅ Function calling fixed (using gpt-4o)
- ✅ Self-correction via retry loop
- ✅ Parallel agent execution
- ✅ Error handling and timeouts

## Technology Stack

### Web Search
- **Primary**: Tavily API (recommended for LLM agents)
- **Alternative**: Serper API (Google Search)
- **Fallback**: Direct HTTP requests to search engines

### Fintwit Analysis
- **Primary**: Twitter API v2 (if available)
- **Alternative**: Web Search Agent to find fintwit content
- **Sentiment Analysis**: Use LLM to analyze found content

### Market Data
- **Primary**: Finnhub API (free tier: 60 calls/min, 1,000,000 calls/month)
- **Backup**: Yahoo Finance API (unofficial)
- **Real-time**: Fresh data fetched on each request (no caching)

## Agent Workflow Example

```
User: "What do you think about my portfolio?"

1. Orchestrator receives query
2. Orchestrator automatically calls:
   - UserProfileAgent.getPortfolio(userId) → Get holdings
   - UserProfileAgent.getUserProfile(userId) → Get preferences
   
3. For each stock in portfolio:
   - MarketDataAgent.getStockPrice(symbol) → Current price
   - MarketDataAgent.analyzeTrends(symbol) → Technical analysis
   - WebSearchAgent.searchStockAnalysis(symbol) → Latest analysis
   - FintwitAnalysisAgent.analyzeFintwitSentiment(symbol) → Social sentiment
   
4. Orchestrator synthesizes all data
5. Provides comprehensive portfolio analysis
```

## Benefits

1. **Comprehensive Data**: Web search + fintwit + market data = complete picture
2. **Real-time Insights**: Latest news and social sentiment
3. **Better Analysis**: Multiple data sources for more accurate recommendations
4. **Professional Quality**: Research-grade analysis with multiple perspectives

## Current Architecture

The system is **fully implemented** with:
- ✅ Plan-Execute-Evaluate orchestrator with self-correction
- ✅ 7 specialized agents (Planner, Evaluator, UserProfile, MarketAnalysis, WebSearch, Fintwit, Security)
- ✅ Real-time market data via Finnhub
- ✅ Web search via Tavily/Serper
- ✅ Social sentiment analysis
- ✅ Google OAuth2 authentication
- ✅ Portfolio and profile management
- ✅ WebSocket real-time updates

## Future Enhancements

Potential improvements for future iterations:
1. Add market data caching for performance
2. Implement recommendation generation service
3. Add more technical indicators
4. Enhanced portfolio analytics
5. Multi-currency support

