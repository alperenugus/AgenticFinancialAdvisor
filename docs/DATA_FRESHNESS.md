# Data Freshness & Real-Time Data

## Current Implementation

### ✅ Fresh Data Fetching (No Cache)

The system fetches market data from **Finnhub** on each request:

- No application-level cache in `MarketDataService`
- Fresh HTTP request per tool invocation (`getStockPrice`, `getStockPriceData`, `getMarketNews`)
- Data is not reused between requests

## Data Sources

### Finnhub (Primary)

- **Current Price** (`/quote`)
- **Candles / historical aggregates** (`/stock/candle`)
- **Company profile** (`/stock/profile2`)
- **Company news** (`/company-news`)

### Symbol Resolution (Agentic Consensus)

Ticker/company mapping is resolved from live candidates using:

- Finnhub quote/search candidate generation
- Multi-agent consensus (`LlmTickerResolver`):
  - planner
  - selector
  - evaluator
  - auditor

If consensus is not reached, the system returns unresolved instead of guessing.

### Optional Additional Sources

- Yahoo Finance (configured in app settings, not primary)
- External web/news providers via WebSearchAgent tools

## Important Limitations

### 1) Provider Delay

Market-data providers may have delayed quotes depending on exchange coverage and plan tier.
Even when requests are fresh, returned prices can still be delayed by provider policy.

### 2) API Limits

When quota/rate limits are hit:

- API responses may fail
- Tool calls may return errors or partial data
- The assistant should state that live data was unavailable

### 3) Market Hours / Non-trading Periods

- Outside market hours, “latest” price may reflect the last trade/close
- Weekends/holidays return last available market session values

## How Freshness Works in This App

Example flow for `getStockPrice("AAPL")`:

1. Agent tool is called
2. `MarketDataService.getStockPrice("AAPL")` executes
3. A fresh request is sent to Finnhub
4. Response is parsed and returned immediately
5. No cache lookup is used

## Operational Recommendations

### For Development

- Current setup is fine for testing and functional validation

### For Production

- Add clear UI labeling for fetched timestamp (`fetchedAt`) and provider delay caveat
- Add fallback provider strategy if Finnhub is unavailable
- Add monitoring on rate-limit and timeout error rates
- Consider tier upgrades if you need stricter real-time guarantees

## Summary

| Area | Status |
|---|---|
| Fresh request per call | ✅ Yes |
| App-level market-data cache | ❌ No |
| Provider-side delay possible | ⚠️ Yes |
| Rate-limit impact possible | ⚠️ Yes |

Key point: the app does not serve cached market data by default. If stale values appear, the most likely causes are provider delay, quota/rate-limit failures, or missing API configuration.

