# API Documentation

## Base URL

- **Local Development**: `http://localhost:8080/api`
- **Production**: `https://your-backend.railway.app/api`

## Authentication

The API uses **JWT (JSON Web Tokens)** for authentication. All endpoints (except `/api/auth/**` and `/oauth2/**`) require a valid JWT token in the `Authorization` header.

### Getting a Token

1. **Via Frontend**: Sign in with Google through the frontend - token is automatically stored
2. **Via OAuth2 Flow**: Redirect to `/oauth2/authorization/google` - completes Google OAuth2 and redirects with token
3. **Validate Token**: Use `/api/auth/validate` to check token validity

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
    "userProfileAgent": true,
    "marketAnalysisAgent": true,
    "riskAssessmentAgent": true,
    "researchAgent": true,
    "recommendationAgent": true,
    "chatLanguageModel": true
  }
}
```

**Note:** This endpoint is public (no authentication required) for health checks.

### Debug Recommendations

```http
GET /api/advisor/debug-recommendations
Authorization: Bearer <token>
```

**Response:**
```json
{
  "portfolioHoldings": ["AAPL", "NVDA", "MSFT", "GOOGL"],
  "portfolioHoldingsCount": 4,
  "recommendationsCount": 4,
  "recommendationsBySymbol": {
    "AAPL": ["BUY"],
    "NVDA": ["HOLD"],
    "MSFT": ["BUY"],
    "GOOGL": ["HOLD"]
  },
  "recommendations": [
    {
      "id": 1,
      "symbol": "AAPL",
      "action": "BUY",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

**Note:**
- Useful for troubleshooting why recommendations aren't showing
- Shows all stocks in portfolio vs. recommendations in database
- Helps identify missing recommendations or duplicates
- Shows recommendation count per symbol

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

### User (OAuth2)

```typescript
interface User {
  id: number;
  email: string;        // Unique identifier
  name: string;
  googleId: string;      // Google OAuth2 user ID
  pictureUrl: string;   // Profile picture URL
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

### Alpha Vantage API
- **Free Tier**: 5 calls per minute, 500 calls per day
- The system implements basic rate limiting awareness

### Recommendations
- No rate limiting currently implemented
- Recommended: 10 requests per minute per user

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

// 7. Get recommendations
const recommendations = await fetch('/api/advisor/recommendations', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
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

### RiskAssessmentAgent Tools
- `assessStockRisk(symbol: string, metrics: string): string`
- `calculatePortfolioRisk(userId: string): string`
- `checkRiskTolerance(userId: string, symbol: string): string`
- `getRiskMetrics(symbol: string): string`

### ResearchAgent Tools
- `getCompanyFundamentals(symbol: string): string`
- `analyzeFinancials(symbol: string): string`
- `compareCompanies(symbols: string): string`
- `getSectorAnalysis(symbol: string): string`

### RecommendationAgent Tools
- `generateRecommendation(userId, symbol, marketAnalysis, riskAssessment, researchSummary, userProfile): string`
- `explainReasoning(components: string): string`
- `calculateConfidence(factors: string): string`
- `formatRecommendation(recommendation: string): string`

### StockDiscoveryAgent Tools
- `discoverStocks(riskTolerance: string, sectors: string, excludeOwned: string): string` - Discover stocks matching criteria with real-time validation (no hardcoded lists)
- `validateStockSymbol(symbol: string): string` - Validate if a stock symbol exists and is tradeable

---

## Changelog

### v2.4.0 (Current)
- **Removed Recommendations Tab** - Recommendations feature temporarily disabled, will be re-enabled in future iteration
- **AI Advisor Portfolio & Profile Access** - Confirmed AI Advisor has full access to user portfolio and profile via UserProfileAgent tools
- **Simplified UI** - Removed recommendation-related UI components and endpoints

### v2.3.0
- **Fixed Model Error** - Reverted from decommissioned `mixtral-8x7b-32768` to `llama-3.3-70b-versatile`
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
