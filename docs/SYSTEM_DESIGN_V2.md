# System Design V2: Enhanced Agent Architecture

## Overview

This document outlines the improved agent architecture for the Financial Advisor AI system, incorporating web search, fintwit analysis, and enhanced real-time market data capabilities.

## Current Issues

1. **Function Calling Problems**: Model outputs function calls as text instead of executing them
2. **Limited Data Sources**: Only Alpha Vantage API for market data
3. **No Web Search**: Cannot search for latest financial news, analysis, or market insights
4. **No Social Sentiment**: Missing fintwit (financial Twitter) analysis for market sentiment
5. **Agent Coordination**: Agents not working smoothly together

## Proposed Agent Architecture

### Core Agents

```
┌─────────────────────────────────────────────────────────────┐
│                    Orchestrator Agent                        │
│              (llama-3.3-70b-versatile)                      │
│         Coordinates all agents, synthesizes responses         │
└───────────────┬─────────────────────────────────────────────┘
                │
    ┌───────────┼───────────┬───────────┬───────────┐
    │           │           │           │           │
    ▼           ▼           ▼           ▼           ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ User   │ │Market │ │  Web   │ │Fintwit │ │  Risk  │
│Profile │ │ Data  │ │ Search │ │Analysis│ │Assessment│
│ Agent  │ │ Agent │ │ Agent  │ │ Agent  │ │ Agent  │
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘
    │           │           │           │           │
    └───────────┼───────────┼───────────┼───────────┘
                │           │           │
                ▼           ▼           ▼
         ┌──────────────────────────────┐
         │    Research Agent             │
         │  (Synthesizes all data)       │
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
- `getStockPrice(symbol)` - Current stock price (Alpha Vantage)
- `getStockPriceData(symbol, timeframe)` - Historical price data
- `getMarketNews(symbol)` - Market news (Alpha Vantage)
- `analyzeTrends(symbol, timeframe)` - Trend analysis
- `getTechnicalIndicators(symbol)` - Technical indicators

**Enhancements Needed**:
- Add Yahoo Finance as backup data source
- Add real-time price streaming capability
- Improve technical indicator calculations

**Status**: ✅ Implemented, needs enhancement

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

### 5. Risk Assessment Agent (Existing)
**Purpose**: Evaluate risk levels

**Tools**:
- `assessStockRisk(symbol, metrics)` - Individual stock risk
- `calculatePortfolioRisk(userId)` - Portfolio risk
- `checkRiskTolerance(userId, symbol)` - Compare with user tolerance
- `getRiskMetrics(symbol)` - Risk metrics (volatility, beta)

**Status**: ✅ Already implemented

### 6. Research Agent (Existing - Enhanced)
**Purpose**: Fundamental analysis and company research

**Current Tools**:
- `getCompanyFundamentals(symbol)` - Financial ratios
- `analyzeFinancials(symbol)` - Financial analysis
- `compareCompanies(symbols)` - Company comparison
- `getSectorAnalysis(symbol)` - Sector analysis

**Enhancements**:
- Integrate with Web Search Agent for latest research
- Add earnings analysis
- Add analyst ratings

**Status**: ✅ Implemented, can be enhanced

### 7. Orchestrator Agent (Existing - Needs Fix)
**Purpose**: Coordinate all agents

**Current Issues**:
- Model outputs function calls as text
- Not using agents effectively
- Poor coordination between agents

**Fixes Needed**:
- Better system message (already updated)
- Ensure tools are called automatically
- Better error handling
- Improved agent coordination logic

## Implementation Plan

### Phase 1: Web Search Agent
1. Sign up for Tavily API (https://tavily.com)
2. Create `WebSearchAgent.java` with Tavily integration
3. Add tools for financial news, stock analysis, market trends
4. Integrate with Orchestrator

### Phase 2: Fintwit Analysis Agent
1. Evaluate Twitter API access
2. If available: Create `FintwitAnalysisAgent.java` with Twitter API
3. If not: Use Web Search Agent as fallback to find fintwit content
4. Add sentiment analysis tools
5. Integrate with Orchestrator

### Phase 3: Enhanced Market Data
1. Add Yahoo Finance as backup source
2. Improve technical indicators
3. Add real-time streaming capability

### Phase 4: Orchestrator Improvements
1. Fix function calling issues (in progress)
2. Improve agent coordination
3. Add better error handling
4. Optimize tool selection logic

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
- **Primary**: Alpha Vantage API
- **Backup**: Yahoo Finance API
- **Real-time**: WebSocket connections (future)

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
   - FintwitAnalysisAgent.getFintwitSentiment(symbol) → Social sentiment
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

## Next Steps

1. Implement Web Search Agent with Tavily
2. Implement Fintwit Analysis Agent (Twitter API or web search fallback)
3. Enhance existing agents
4. Fix orchestrator function calling
5. Test end-to-end workflow
6. Update documentation

