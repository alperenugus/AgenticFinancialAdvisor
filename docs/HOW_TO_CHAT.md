# How to Chat with the Financial Advisor

## Architecture (what must be running)

You interact with the **Frontend**.  
Frontend calls the **Backend** (REST + WebSocket).  
Backend calls:
- Groq models (`llama-3.3-70b-versatile` orchestrator, `llama-3.1-8b-instant` subagents)
- Finnhub market-data API

## Required services

1. Backend service
2. Frontend service
3. PostgreSQL (or local H2 profile for development)

## Required environment variables

### Backend

```bash
GROQ_API_KEY=your_groq_key
FINNHUB_API_KEY=your_finnhub_key
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
JWT_SECRET=your_32_plus_char_secret
CORS_ORIGINS=https://your-frontend-url
```

### Frontend

```bash
VITE_API_BASE_URL=https://your-backend-url/api
VITE_WS_URL=https://your-backend-url/ws
```

## Chat flow

1. Open frontend app
2. Sign in with Google
3. Go to chat tab
4. Ask a question (example: “What is the latest price of AAPL?”)
5. Backend orchestrator delegates to relevant subagents and returns a final answer

## Troubleshooting

### No response / timeout
- Check backend logs for timeout messages
- Verify `GROQ_API_KEY`
- Verify backend can reach Groq and Finnhub

### Market price looks stale or missing
- Verify `FINNHUB_API_KEY`
- Check provider quota/rate-limit status
- Confirm market is open (latest value may reflect last trade/close)

### Frontend cannot connect
- Ensure `VITE_API_BASE_URL` and `VITE_WS_URL` use public URLs
- Do not use private/internal service hostnames in browser configs

## Quick backend status check

```bash
curl https://your-backend-url/api/advisor/status
```

