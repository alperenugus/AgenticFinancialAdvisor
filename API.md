# API Documentation

## Base URL

- **Local Development**: `http://localhost:8080/api`
- **Production**: `https://agenticfinancialadvisorbackend-production.up.railway.app/api`

## Authentication

The API uses **JWT (JSON Web Tokens)** for authentication. All endpoints except `/api/auth/**`, `/api/health`,
`/api/advisor/status`, `/login/**`, and `/oauth2/**` require a valid JWT in the `Authorization` header.

Two ways to obtain a token:

- **Email + password** — `POST /api/auth/register` (sign up) or `POST /api/auth/login` (sign in). Both return
  `{ token, user }`.
- **Google OAuth2** — redirect the browser to `/oauth2/authorization/google`; on success the backend
  redirects to `<frontend>/auth/callback?token=<jwt>`.

Use `/api/auth/validate` to check a token's validity.

### Using Tokens

Include the token in the `Authorization` header:

```http
Authorization: Bearer <your_jwt_token>
```

### Token Expiration

- Default: 24 hours (configurable via `JWT_EXPIRATION`)
- Expired tokens return `401 Unauthorized`
- Frontend automatically redirects to login on 401

---

## Authentication Endpoints

### Register (email/password)

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "jane@example.com",
  "name": "Jane Investor",
  "password": "at-least-8-chars"
}
```

**Response (201 Created):**
```json
{
  "token": "<jwt>",
  "user": { "id": 5, "email": "jane@example.com", "name": "Jane Investor", "pictureUrl": null }
}
```

**Errors:** `400` invalid email / password &lt; 8 chars; `409` an account with that email already exists.

### Login (email/password)

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "jane@example.com",
  "password": "at-least-8-chars"
}
```

**Response (200):** same `{ token, user }` shape as register.

**Errors:** `401` for unknown email, wrong password, or a Google-only account (no local password set). The
message is intentionally generic ("Invalid email or password.") to avoid leaking which emails are registered.

### Get Current User

```http
GET /api/auth/me
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "pictureUrl": "https://lh3.googleusercontent.com/..."
}
```

### Validate Token

```http
POST /api/auth/validate
Authorization: Bearer <token>
```

**Response:**
```json
{
  "valid": true,
  "email": "user@example.com",
  "userId": 1,
  "name": "John Doe"
}
```

**Error Response (401):**
```json
{
  "status": "error",
  "message": "Invalid or expired token"
}
```

---

## User Profile Endpoints

All profile endpoints use the authenticated user's email (from JWT token) - no `userId` parameter needed.

### Get User Profile

```http
GET /api/profile
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": 1,
  "userId": "user@example.com",
  "riskTolerance": "MODERATE",
  "horizon": "MEDIUM",
  "goals": ["GROWTH", "RETIREMENT"],
  "budget": 10000.00,
  "preferredSectors": ["Technology", "Healthcare"],
  "excludedSectors": [],
  "ethicalInvesting": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Create User Profile

```http
POST /api/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "riskTolerance": "MODERATE",
  "horizon": "MEDIUM",
  "goals": ["GROWTH", "RETIREMENT"],
  "budget": 10000,
  "preferredSectors": ["Technology"],
  "excludedSectors": [],
  "ethicalInvesting": false
}
```

**Response:** Created user profile (same structure as GET)

**Note:** `userId` is automatically set from the authenticated user's email.

### Update User Profile

```http
PUT /api/profile
Authorization: Bearer <token>
Content-Type: application/json

{
  "riskTolerance": "AGGRESSIVE",
  "budget": 15000
}
```

**Response:** Updated user profile

---

## Portfolio Management

All portfolio endpoints use the authenticated user - no `userId` parameter needed.

### Get Portfolio

```http
GET /api/portfolio
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": 1,
  "userId": "user@example.com",
  "holdings": [
    {
      "id": 1,
      "symbol": "AAPL",
      "quantity": 10,
      "averagePrice": 150.00,
      "currentPrice": 155.00,
      "value": 1550.00,
      "gainLoss": 50.00,
      "gainLossPercent": 3.33
    }
  ],
  "totalValue": 1550.00,
  "totalGainLoss": 50.00,
  "totalGainLossPercent": 3.33,
  "lastUpdated": "2024-01-15T10:30:00"
}
```

### Add Holding

```http
POST /api/portfolio/holdings
Authorization: Bearer <token>
Content-Type: application/json

{
  "symbol": "AAPL",
  "quantity": 10,
  "averagePrice": "150.00"
}
```

**Response:**
```json
{
  "message": "Holding added successfully",
  "holding": {
    "id": 1,
    "symbol": "AAPL",
    "quantity": 10,
    "averagePrice": 150.00,
    "currentPrice": 155.00
  },
  "portfolio": { ... }
}
```

### Remove Holding

```http
DELETE /api/portfolio/holdings/{holdingId}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "message": "Holding removed successfully",
  "portfolio": { ... }
}
```

### Refresh Portfolio Prices

```http
POST /api/portfolio/refresh
Authorization: Bearer <token>
```

**Response:** Updated portfolio with current prices

---

## Advisor

### Request Analysis

```http
POST /api/advisor/analyze
Authorization: Bearer <token>
Content-Type: application/json

{
  "query": "Should I buy Apple stock?",
  "sessionId": "session-123"
}
```

**Response:**
```json
{
  "sessionId": "session-123",
  "userId": "user@example.com",
  "response": "Based on my analysis...",
  "status": "success"
}
```

**Note:** 
- `userId` is automatically extracted from JWT token
- This endpoint triggers the orchestrator to coordinate all agents
- Real-time updates are sent via WebSocket
- **The AI Advisor has full access to user's portfolio and profile data** - it automatically checks these before providing advice
- The orchestrator uses UserProfileAgent tools to access:
  - `getUserProfile(userId)` - User's risk tolerance, goals, preferences
  - `getPortfolio(userId)` - Complete portfolio with all holdings and current prices
  - `getPortfolioSummary(userId)` - Portfolio summary (total value, gain/loss, holdings)
  - `getPortfolioHoldings(userId)` - List of stocks user owns
- For greetings (hello, hi, etc.), the agent responds naturally and guides users to financial questions

### Check Agent Status

```http
GET /api/advisor/status
```

**Response:**
```json
{
  "status": "operational",
  "agents": {
    "plannerAgent": true,
    "evaluatorAgent": true,
    "userProfileAgent": true,
    "marketAnalysisAgent": true,
    "webSearchAgent": true,
    "fintwitAnalysisAgent": true,
    "securityAgent": true
  }
}
```

**Note:** This endpoint is **public** (no authentication) and is one of the Railway healthcheck paths.

---

## Health

### Liveness

```http
GET /api/health
```

**Response (200):**
```json
{ "status": "UP", "service": "financial-advisor", "timestamp": "2026-06-09T20:45:00Z" }
```

**Note:** Public, dependency-free liveness probe used by the Railway healthcheck and the Docker
`HEALTHCHECK`. It must stay public — an authenticated healthcheck path 302-redirects to the OAuth login,
which Railway treats as unhealthy and fails the deploy.

---

## WebSocket API

### Connection

WebSocket connections require authentication. Include the JWT token in the connection headers:

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  {
    Authorization: 'Bearer ' + token
  },
  function(frame) {
    console.log('Connected');
  }
);
```

### Topics

#### Thinking Updates
```javascript
stompClient.subscribe('/topic/thinking/{sessionId}', function(message) {
  const data = JSON.parse(message.body);
  // data.type = "thinking"
  // data.content = "Analyzing market trends..."
});
```

#### Final Response
```javascript
stompClient.subscribe('/topic/response/{sessionId}', function(message) {
  const data = JSON.parse(message.body);
  // data.type = "response"
  // data.content = "Based on my analysis..."
});
```

#### Error Messages
```javascript
stompClient.subscribe('/topic/error/{sessionId}', function(message) {
  const data = JSON.parse(message.body);
  // data.type = "error"
  // data.content = "Error message..."
});
```

### Message Format

```json
{
  "type": "thinking|response|error",
  "content": "Message content"
}
```

---

## Data Models

### User

```typescript
interface User {
  id: number;
  email: string;          // Unique identifier
  name: string;
  googleId?: string;      // Google OAuth2 user ID (null for local accounts)
  pictureUrl?: string;    // Profile picture URL
  authProvider?: string;  // "GOOGLE" | "LOCAL"
  // passwordHash is stored (BCrypt) for LOCAL accounts but is NEVER returned by the API.
  createdAt: string;
  updatedAt: string;
}
```

### UserProfile

```typescript
interface UserProfile {
  id: number;
  userId: string;       // User's email (from authentication)
  riskTolerance: "CONSERVATIVE" | "MODERATE" | "AGGRESSIVE";
  horizon: "SHORT" | "MEDIUM" | "LONG";
  goals: string[];      // ["GROWTH", "RETIREMENT", "INCOME"]
  budget: number;
  preferredSectors: string[];
  excludedSectors: string[];
  ethicalInvesting: boolean;
  createdAt: string;    // ISO 8601
  updatedAt: string;    // ISO 8601
}
```

### Portfolio

```typescript
interface Portfolio {
  id: number;
  userId: string;       // User's email
  holdings: StockHolding[];
  totalValue: number;
  totalGainLoss: number;
  totalGainLossPercent: number;
  lastUpdated: string;
}

interface StockHolding {
  id: number;
  symbol: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  value: number;
  gainLoss: number;
  gainLossPercent: number;
  lastUpdated: string;
}
```

### Recommendation

```typescript
interface Recommendation {
  id: number;
  userId: string;       // User's email
  symbol: string;
  action: "BUY" | "SELL" | "HOLD";
  confidence: number;  // 0.0 - 1.0
  reasoning: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  targetPrice: number;
  timeHorizon: "SHORT" | "MEDIUM" | "LONG";
  marketAnalysis: string;
  riskAssessment: string;
  researchSummary: string;
  // Professional Financial Analyst Fields
  stopLossPrice?: number;           // Stop loss price level
  entryPrice?: number;               // Recommended entry price
  exitPrice?: number;                // Recommended exit price
  technicalPatterns?: string;        // Technical analysis patterns (e.g., "head and shoulders pattern on monthly")
  averagingDownAdvice?: string;      // Averaging down strategy advice
  professionalAnalysis?: string;    // Professional financial analyst analysis
  createdAt: string;
}
```

---

## Error Responses

### Standard Error Format

```json
{
  "status": "error",
  "message": "Error description"
}
```

### HTTP Status Codes

- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Invalid request
- `401 Unauthorized` - Authentication required or token invalid
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

### Example Error Responses

**401 Unauthorized (No Token):**
```json
{
  "status": "error",
  "message": "Authentication required"
}
```

**401 Unauthorized (Invalid Token):**
```json
{
  "status": "error",
  "message": "Invalid or expired token"
}
```

**400 Bad Request:**
```json
{
  "status": "error",
  "message": "Query is required"
}
```

**404 Not Found:**
```json
{
  "status": "error",
  "message": "User profile not found"
}
```

**500 Internal Server Error:**
```json
{
  "status": "error",
  "message": "Error processing request: [details]"
}
```

---

## Rate Limiting

### Market data (Finnhub + Yahoo Finance)
- **Finnhub Free Tier**: 60 calls/minute, 1,000,000 calls/month — used for live quotes and company news.
- **Yahoo Finance fallback** (no API key): used automatically for live quotes when Finnhub is missing/limited,
  and for historical/candle data (Finnhub's `/stock/candle` is premium-only and 403s on the free tier).
- **Short-TTL quote cache** (~15s, configurable via `MARKET_DATA_QUOTE_CACHE_TTL_SECONDS`) protects the free
  tier without serving stale prices. Quote responses include `quoteTime` (provider timestamp) and `source`.

### OpenAI API
- Rate limits depend on your OpenAI plan (per-minute RPM/TPM; not a daily cap). On a `429` the advisor returns
  a brief "rate-limited, try again in a few moments" message rather than erroring.
- Models are tiered and env-overridable (`OPENAI_*_MODEL`): `gpt-4o` for the orchestrator (planner + evaluator)
  and tool-calling sub-agents, `gpt-4o-mini` for the security gate.

### Application Rate Limiting
- Per-session rate limiting implemented using token bucket algorithm
- Default: 20 requests per session
- Configurable via `RATE_LIMIT_CAPACITY` environment variable

---

## Example Usage

### Complete Flow Example

```javascript
// 1. Authenticate (via frontend OAuth2 flow)
// Token is stored in localStorage after Google Sign-In

const token = localStorage.getItem('token');

// 2. Get current user
const userResponse = await fetch('/api/auth/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const user = await userResponse.json();

// 3. Create user profile
const profile = await fetch('/api/profile', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    riskTolerance: 'MODERATE',
    horizon: 'MEDIUM',
    goals: ['GROWTH'],
    budget: 10000
  })
});

// 4. Add stock to portfolio
await fetch('/api/portfolio/holdings', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    symbol: 'AAPL',
    quantity: 10,
    averagePrice: '150.00'
  })
});

// 5. Request analysis
const analysis = await fetch('/api/advisor/analyze', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    query: 'Should I buy more Apple stock?',
    sessionId: 'session-123'
  })
});

// 6. Connect to WebSocket for real-time updates
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: `Bearer ${token}` },
  () => {
    stompClient.subscribe('/topic/thinking/session-123', (message) => {
      const data = JSON.parse(message.body);
      console.log('Thinking:', data.content);
    });
    
    stompClient.subscribe('/topic/response/session-123', (message) => {
      const data = JSON.parse(message.body);
      console.log('Response:', data.content);
    });
  }
);
```

---

## Agent Tools (Internal)

These are the tools available to the LLM orchestrator. They're called automatically based on the user query. The `userId` parameter is automatically extracted from the authenticated user's email.

### UserProfileAgent Tools
- `getUserProfile(userId: string): string` - Gets profile for authenticated user (risk tolerance, goals, preferences)
- `updateRiskTolerance(userId: string, riskTolerance: string): string` - Updates user's risk tolerance
- `getInvestmentGoals(userId: string): string` - Gets user's investment goals
- `getPortfolio(userId: string): string` - Gets complete portfolio with all holdings, values, and gain/loss (auto-refreshes prices)
- `getPortfolioHoldings(userId: string): string` - Gets list of stocks user owns with quantities
- `getPortfolioSummary(userId: string): string` - Gets portfolio summary (total value, gain/loss, holdings count, symbols)

### MarketAnalysisAgent Tools
- `getStockPrice(symbol: string): string`
- `getStockPriceData(symbol: string, timeframe: string): string`
- `getMarketNews(symbol: string): string`
- `analyzeTrends(symbol: string, timeframe: string): string`
- `getTechnicalIndicators(symbol: string): string`

### WebSearchAgent Tools
- `searchFinancialNews(query: string): string` - Search for financial news, analysis, and market insights
- `searchStockAnalysis(symbol: string): string` - Search for stock analysis and research reports
- `searchMarketTrends(query: string): string` - Search for market trends and sector analysis
- `searchCompanyInfo(symbol: string): string` - Search for company information, earnings, and corporate news

### FintwitAnalysisAgent Tools
- `analyzeFintwitSentiment(symbol: string): string` - Analyze social sentiment from financial Twitter
- `getFintwitTrends(query: string): string` - Get trending topics from financial Twitter
- `searchFintwitContent(query: string): string` - Search for specific content on financial Twitter

---

## Changelog

### v2.7.0 (Current)
- **LLM provider switched from Groq to OpenAI.** Uses OpenAI's Chat Completions API via LangChain4j.
  Models are tiered and env-overridable (`OPENAI_API_KEY` + `OPENAI_ORCHESTRATOR_MODEL` / `OPENAI_AGENT_MODEL`
  / `OPENAI_SECURITY_MODEL`): defaults `gpt-4o` (orchestrator + sub-agents), `gpt-4o-mini` (security). No more
  daily-token wall; cost is pay-as-you-go.
- **Portfolio total bug fixed** — `totalValue` no longer persists as `$0` when only holding prices change;
  totals are recomputed explicitly on every read/refresh/add/remove path.
- **Market overview** — broad questions ("will markets recover") now use real S&P/Dow/Nasdaq/VIX index data.
- **Web search** — bounded retry around the Tavily call (fixes intermittent first-call timeouts).

### v2.6.0 (Current)
- **Anti-hallucination grounding gate** — every figure in an advisor response is deterministically verified
  against raw tool output (`GroundingService`); violations trigger a corrective rewrite, and unverifiable
  figures ship with an explicit caution. Grounding verdicts stream over the WebSocket as `grounding` events
  ("Fact Check" in the UI).
- **Raw tool-data capture** — `ToolCallAspect` records every tool result per session; the evaluator now
  synthesizes from raw data (with `quoteTime`/`source`/`asOf`), not just sub-agent paraphrases.
- **Deterministic personalization** — user profile (risk tolerance, horizon, goals, budget, sectors, ESG) +
  portfolio allocation/concentration are injected into planning and answer synthesis on every query.
- **Real technical indicators** — `getTechnicalIndicators`/`analyzeTrends` now return SMA20/50, RSI14,
  30-day annualized volatility, 1m/3m/1y returns, and 52-week high/low computed from one year of daily
  candles, each stamped with `asOf` + `source` + the classification rule used.
- **directResponse hardening** — planner direct responses are honored only for greetings without figures.
- Advice responses end with an education-not-advice disclaimer line.

### v2.5.0
- **Email/password authentication** — `POST /api/auth/register` and `POST /api/auth/login` (BCrypt-hashed),
  alongside the existing Google OAuth2. Returns `{ token, user }`.
- **Public `/api/health` liveness endpoint** and `/api/advisor/status` made public — fixes the Railway
  healthcheck (an auth-protected path was 302-redirecting and failing every deploy).
- **Market-data freshness** — Yahoo Finance fallback for live quotes and historical candles (the Finnhub
  premium `/stock/candle` 403s on free tier), short-TTL quote cache, and `quoteTime`/`source` in responses.
- **Security** — removed the hardcoded JWT fallback secret; the app now fails fast at startup unless a strong
  `JWT_SECRET` is configured.
- Removed documentation for the non-existent `/api/advisor/debug-recommendations` and
  `/api/advisor/recommendations` endpoints.

### v2.4.0
- **Removed Recommendations Tab** - Recommendations feature temporarily disabled, will be re-enabled in future iteration
- **AI Advisor Portfolio & Profile Access** - Confirmed AI Advisor has full access to user portfolio and profile via UserProfileAgent tools
- **Simplified UI** - Removed recommendation-related UI components and endpoints

### v2.3.0
- **Fixed Model Error** - Reverted from decommissioned `mixtral-8x7b-32768` to `gpt-4o`
- **Increased Timeout** - Orchestrator timeout increased to 90 seconds for comprehensive analysis
- **Improved Function Calling** - Fixed function calling errors with better system message instructions

### v2.1.0
- **Portfolio Access Tools** - AI agents can now access user portfolio data (getPortfolio, getPortfolioHoldings, getPortfolioSummary)
- **Pre-Generated Recommendations** - Automatic recommendation generation based on user profile and portfolio
- **Intelligent Greeting Handling** - Natural conversation flow with contextual financial guidance
- **Recommendation Generation Service** - Background service for personalized recommendations
- **Async Processing** - Enabled async processing for background tasks
- **Stock Price Graph Favicon** - Professional branding with stock chart icon

### v2.0.0
- **Google OAuth2 Authentication** - Secure passwordless authentication
- **JWT Token-based API** - All endpoints require authentication
- **User Management** - User entity with Google OAuth integration
- **Updated API Endpoints** - Removed `userId` from paths (uses authenticated user)
- All 6 agents implemented
- WebSocket support for real-time updates
- Portfolio management
- User profile management

### v1.0.0
- Initial API release
- Session-based user IDs
- All 6 agents implemented
- WebSocket support for real-time updates
- Portfolio management
- User profile management

---

For architecture details, see [ARCHITECTURE.md](./ARCHITECTURE.md).
For setup instructions, see [README.md](./README.md).
For Google Auth setup, see [docs/GOOGLE_AUTH_SETUP.md](./docs/GOOGLE_AUTH_SETUP.md).
