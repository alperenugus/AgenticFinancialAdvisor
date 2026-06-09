# Data Freshness & Real-Time Data

## Current Implementation

### ✅ Live Data with Short-TTL Quote Cache

The system fetches market data from **Finnhub** with an automatic **Yahoo Finance** fallback:

- Live HTTP request per tool invocation (`getStockPrice`, `getStockPriceData`, `getMarketNews`)
- A short-TTL quote cache (~15s) protects the free tier without serving stale prices
  - TTL is configurable via `MARKET_DATA_QUOTE_CACHE_TTL_SECONDS` (relaxed-binds to `market-data.quote-cache-ttl-seconds`)
- Quote responses include `quoteTime` (the provider's price timestamp) and `source` (`"finnhub"` or `"yahoo"`)

## Data Sources

### Finnhub (Primary, live quotes)

- **Current Price** (`/quote`)
- **Company profile** (`/stock/profile2`)
- **Company news** (`/company-news`)

### Yahoo Finance (Automatic Fallback)

- **Current Price** fallback when Finnhub fails or is rate-limited (`query1.finance.yahoo.com/v8/finance/chart`, no API key)
- **Candles / historical aggregates** — Yahoo is used here because the Finnhub `/stock/candle` endpoint is **premium-only** and returns `403` on the free tier

### Optional Additional Sources

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
3. The short-TTL (~15s) quote cache is checked; a fresh value is returned on hit
4. On a miss, a live request is sent to Finnhub `/quote`
5. If Finnhub fails or is rate-limited, the request falls back to Yahoo Finance
6. The response (including `quoteTime` and `source`) is parsed, cached, and returned

## Operational Recommendations

### For Development

- Current setup is fine for testing and functional validation

### For Production

- Add clear UI labeling for the provider price timestamp (`quoteTime`) and provider delay caveat
- Surface the `source` (`finnhub` / `yahoo`) so users know which provider answered
- Add monitoring on rate-limit and timeout error rates
- Consider tier upgrades if you need stricter real-time guarantees

## Summary

| Area | Status |
|---|---|
| Live request per call (cache miss) | ✅ Yes |
| Short-TTL quote cache (~15s) | ✅ Yes |
| Automatic Yahoo Finance fallback | ✅ Yes |
| Provider-side delay possible | ⚠️ Yes |
| Rate-limit impact possible | ⚠️ Yes |

Key point: quotes are served live, briefly cached (~15s) to protect the free tier, and fall back to Yahoo Finance when Finnhub is unavailable. Historical/candle data always uses Yahoo (Finnhub candles are premium-only). If stale values appear, the most likely causes are provider delay, quota/rate-limit failures, or missing API configuration.

