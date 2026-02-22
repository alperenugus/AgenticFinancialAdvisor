# Running Locally

## Prerequisites

1. Java 21+
2. Maven 3.8+
3. Node.js 18+ (frontend)
4. Groq API key - [Get from Groq Console](https://console.groq.com/)
5. Finnhub API key - [Get from Finnhub](https://finnhub.io/)
6. Google OAuth2 credentials - See [Google Auth Setup Guide](./GOOGLE_AUTH_SETUP.md)

## Backend (quick start with H2)

```bash
cd backend
export GROQ_API_KEY=your_groq_key
export FINNHUB_API_KEY=your_finnhub_key
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
export JWT_SECRET=$(openssl rand -base64 32)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

This profile uses:
- H2 in-memory DB (no PostgreSQL needed)
- Port 8080
- Groq models:
  - All agents: `llama-3.3-70b-versatile` (orchestrator, evaluator, and all sub-agents)

## Backend (PostgreSQL)

```bash
docker run -d --name postgres-financial \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=financialadvisor \
  -p 5432:5432 \
  postgres:15

cd backend
export DATABASE_URL=jdbc:postgresql://localhost:5432/financialadvisor
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export GROQ_API_KEY=your_groq_key
export FINNHUB_API_KEY=your_finnhub_key
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
export JWT_SECRET=$(openssl rand -base64 32)
export GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
export CORS_ORIGINS=http://localhost:5173,http://localhost:3000
mvn spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Default local URLs:
- API: `http://localhost:8080/api`
- WebSocket: `http://localhost:8080/ws`
- Frontend: `http://localhost:5173`

## Health check

```bash
curl http://localhost:8080/api/advisor/status
```

## Important notes

- All API endpoints require authentication (JWT token from Google OAuth2)
- For local development, add `http://localhost:8080/login/oauth2/code/google` to Google OAuth2 authorized redirect URIs
- If market data is missing, verify `FINNHUB_API_KEY`
- If analysis fails to start, verify `GROQ_API_KEY`
- If authentication fails, verify `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `JWT_SECRET`

