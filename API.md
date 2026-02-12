# API Documentation

## Base URL

- **Local Development**: `http://localhost:8080`
- **Production**: (Configure based on deployment)

## Authentication

Currently, the API uses session-based user IDs. Future versions will implement JWT/OAuth2 authentication.

## Endpoints

### User Profile

#### Get User Profile
```http
GET /api/profile/{userId}
```

**Response:**
```json
{
  "id": 1,
  "userId": "user123",
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

#### Create User Profile
```http
POST /api/profile
Content-Type: application/json

{
  "userId": "user123",
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

#### Update User Profile
```http
PUT /api/profile/{userId}
Content-Type: application/json

{
  "riskTolerance": "AGGRESSIVE",
  "budget": 15000
}
```

**Response:** Updated user profile

---

### Portfolio Management

#### Get Portfolio
```http
GET /api/portfolio/{userId}
```

**Response:**
```json
{
  "id": 1,
  "userId": "user123",
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

#### Add Holding
```http
POST /api/portfolio/{userId}/holdings
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

#### Remove Holding
```http
DELETE /api/portfolio/{userId}/holdings/{holdingId}
```

**Response:**
```json
{
  "message": "Holding removed successfully",
  "portfolio": { ... }
}
```

#### Refresh Portfolio Prices
```http
POST /api/portfolio/{userId}/refresh
```

**Response:** Updated portfolio with current prices

---

### Advisor & Recommendations

#### Request Analysis
```http
POST /api/advisor/analyze
Content-Type: application/json

{
  "userId": "user123",
  "query": "Should I buy Apple stock?",
  "sessionId": "session-123"
}
```

**Response:**
```json
{
  "sessionId": "session-123",
  "userId": "user123",
  "response": "Based on my analysis...",
  "status": "success"
}
```

**Note:** This endpoint triggers the orchestrator to coordinate all agents. Real-time updates are sent via WebSocket.

#### Get All Recommendations
```http
GET /api/advisor/recommendations/{userId}
```

**Response:**
```json
[
  {
    "id": 1,
    "userId": "user123",
    "symbol": "AAPL",
    "action": "BUY",
    "confidence": 0.85,
    "reasoning": "Strong fundamentals and positive market trends...",
    "riskLevel": "MEDIUM",
    "targetPrice": 180.00,
    "timeHorizon": "MEDIUM",
    "marketAnalysis": "...",
    "riskAssessment": "...",
    "researchSummary": "...",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

#### Get Specific Recommendation
```http
GET /api/advisor/recommendations/{userId}/{symbol}
```

**Response:** Single recommendation object

#### Check Agent Status
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

---

## WebSocket API

### Connection

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected');
});
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

### UserProfile

```typescript
interface UserProfile {
  id: number;
  userId: string;
  riskTolerance: "CONSERVATIVE" | "MODERATE" | "AGGRESSIVE";
  horizon: "SHORT" | "MEDIUM" | "LONG";
  goals: string[]; // ["GROWTH", "RETIREMENT", "INCOME"]
  budget: number;
  preferredSectors: string[];
  excludedSectors: string[];
  ethicalInvesting: boolean;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}
```

### Portfolio

```typescript
interface Portfolio {
  id: number;
  userId: string;
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
  userId: string;
  symbol: string;
  action: "BUY" | "SELL" | "HOLD";
  confidence: number; // 0.0 - 1.0
  reasoning: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  targetPrice: number;
  timeHorizon: "SHORT" | "MEDIUM" | "LONG";
  marketAnalysis: string;
  riskAssessment: string;
  researchSummary: string;
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
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

### Example Error Responses

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
// 1. Create user profile
const profile = await fetch('/api/profile', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: 'user123',
    riskTolerance: 'MODERATE',
    horizon: 'MEDIUM',
    goals: ['GROWTH'],
    budget: 10000
  })
});

// 2. Add stock to portfolio
await fetch('/api/portfolio/user123/holdings', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    symbol: 'AAPL',
    quantity: 10,
    averagePrice: '150.00'
  })
});

// 3. Request analysis
const analysis = await fetch('/api/advisor/analyze', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: 'user123',
    query: 'Should I buy more Apple stock?',
    sessionId: 'session-123'
  })
});

// 4. Connect to WebSocket for real-time updates
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  stompClient.subscribe('/topic/thinking/session-123', (message) => {
    const data = JSON.parse(message.body);
    console.log('Thinking:', data.content);
  });
  
  stompClient.subscribe('/topic/response/session-123', (message) => {
    const data = JSON.parse(message.body);
    console.log('Response:', data.content);
  });
});

// 5. Get recommendations
const recommendations = await fetch('/api/advisor/recommendations/user123');
```

---

## Agent Tools (Internal)

These are the tools available to the LLM orchestrator. They're called automatically based on the user query.

### UserProfileAgent Tools
- `getUserProfile(userId: string): string`
- `updateRiskTolerance(userId: string, riskTolerance: string): string`
- `getInvestmentGoals(userId: string): string`

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

---

## Changelog

### v1.0.0 (Current)
- Initial API release
- All 6 agents implemented
- WebSocket support for real-time updates
- Portfolio management
- User profile management

---

For architecture details, see [ARCHITECTURE.md](./ARCHITECTURE.md).
For setup instructions, see [README.md](./README.md).

