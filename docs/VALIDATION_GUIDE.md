# Input Validation Guide

This document describes all input validation checks throughout the application.

## Stock Symbol Validation

### Portfolio - Add Holding (`POST /api/portfolio/holdings`)

**Validations:**
1. ✅ **Format validation**: Symbol must be 1-10 alphanumeric uppercase characters (regex: `^[A-Z0-9]{1,10}$`)
2. ✅ **Existence validation**: Symbol must exist in the market (validated by fetching current price from Alpha Vantage)
3. ✅ **Null/empty check**: Symbol cannot be null or empty
4. ✅ **Quantity validation**: Must be a positive integer > 0
5. ✅ **Average price validation**: Must be a positive number > 0

**Error Responses:**
- `400 Bad Request`: Invalid symbol format
- `400 Bad Request`: Symbol does not exist or cannot be found
- `400 Bad Request`: Invalid quantity or average price

**Example:**
```json
// Valid
{
  "symbol": "AAPL",
  "quantity": 10,
  "averagePrice": 150.00
}

// Invalid - symbol doesn't exist
{
  "symbol": "INVALID123",
  "quantity": 10,
  "averagePrice": 150.00
}
// Response: 400 - "Invalid stock symbol: 'INVALID123'. The symbol does not exist or could not be found."
```

### Market Data Service

**`getStockPrice(String symbol)`:**
- ✅ Checks for Alpha Vantage "Error Message" field
- ✅ Checks for Alpha Vantage "Note" field (rate limiting)
- ✅ Validates price data exists in response
- ✅ Returns `null` if symbol is invalid or error occurs

**`validateSymbol(String symbol)`:**
- ✅ Format validation (1-10 alphanumeric uppercase)
- ✅ Existence validation (fetches price to verify)

## User Profile Validation

### Create/Update Profile (`POST /api/profile`, `PUT /api/profile`)

**Validations:**
1. ✅ **Risk Tolerance**: Must be one of: `CONSERVATIVE`, `MODERATE`, `AGGRESSIVE`
   - Invalid values default to `MODERATE` (create) or are skipped (update)
2. ✅ **Investment Horizon**: Must be one of: `SHORT`, `MEDIUM`, `LONG`
   - Invalid values default to `MEDIUM` (create) or are skipped (update)
3. ✅ **Budget**: Must be a valid number (if provided)
4. ✅ **Goals**: Must be an array of strings (if provided)
5. ✅ **Sectors**: Must be arrays of strings (if provided)

**Missing Validations:**
- ❌ Budget range limits (e.g., max value)
- ❌ Goals enum validation (should be from predefined list)
- ❌ Sector names validation (should be from predefined list)

## Advisor/Analysis Validation

### Analyze Query (`POST /api/advisor/analyze`)

**Validations:**
1. ✅ **Query required**: Query cannot be null or empty
2. ✅ **User authentication**: User must be authenticated (JWT token)

**Missing Validations:**
- ❌ Query length limits (max characters)
- ❌ Query content validation (e.g., prevent SQL injection patterns)
- ❌ Session ID format validation

## Authentication Validation

### Token Validation (`POST /api/auth/validate`)

**Validations:**
1. ✅ **Authorization header**: Must start with "Bearer "
2. ✅ **Token format**: Must be valid JWT
3. ✅ **Token expiration**: Checks if token is expired
4. ✅ **User existence**: Validates user exists in database

## Database URL Validation

### DataSource Configuration

**Validations:**
1. ✅ **URL format**: Validates `DATABASE_URL` format
2. ✅ **Error handling**: Throws exception with clear message if invalid

## Missing Validations (Recommendations)

### High Priority

1. **Stock Symbol Validation in Agents**
   - `MarketAnalysisAgent`, `WebSearchAgent`, `FintwitAnalysisAgent` should validate symbols before processing
   - Currently they handle errors gracefully but don't prevent invalid symbols upfront

2. **Query Input Sanitization**
   - `AdvisorController.analyze()` should validate/sanitize user queries
   - Prevent extremely long queries
   - Basic XSS prevention

3. **Budget Limits**
   - User profile budget should have reasonable min/max limits
   - Prevent negative values

### Medium Priority

1. **Enum Validation**
   - Goals should be validated against predefined list
   - Sectors should be validated against predefined list

2. **Rate Limiting**
   - Add rate limiting for API endpoints
   - Prevent abuse of market data API

3. **Input Length Limits**
   - Symbol length (already done)
   - Query length limits
   - Name/description fields

### Low Priority

1. **Format Validation**
   - Email format validation (currently handled by OAuth2)
   - Date format validation (if dates are added)

## Validation Best Practices

### Current Approach
- Manual validation in controllers
- Error messages returned as JSON
- HTTP status codes: 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Internal Server Error)

### Recommended Improvements
1. Use `@Valid` annotations with DTOs
2. Create custom validators for complex validation
3. Centralize validation error handling
4. Add validation tests

## Testing Validation

To test stock symbol validation:

```bash
# Valid symbol
curl -X POST http://localhost:8080/api/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "AAPL", "quantity": 10, "averagePrice": 150.00}'

# Invalid symbol (doesn't exist)
curl -X POST http://localhost:8080/api/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "INVALID123", "quantity": 10, "averagePrice": 150.00}'
# Expected: 400 Bad Request with error message

# Invalid format
curl -X POST http://localhost:8080/api/portfolio/holdings \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"symbol": "aa-pl", "quantity": 10, "averagePrice": 150.00}'
# Expected: 400 Bad Request - "Invalid symbol format"
```

