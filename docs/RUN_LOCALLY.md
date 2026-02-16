# Running Locally

## Prerequisites

1. Java 21+
2. Maven 3.8+
3. Node.js 18+ (frontend)
4. Groq API key
5. Finnhub API key

## Backend (quick start with H2)

```bash
cd backend
export GROQ_API_KEY=your_groq_key
export FINNHUB_API_KEY=your_finnhub_key
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

This profile uses:
- H2 in-memory DB
- Port 8080
- Groq models:
  - Orchestrator: `llama-3.3-70b-versatile`
  - Subagents: `llama-3.1-8b-instant`

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

- `/api/advisor/analyze` requires authentication (JWT/OAuth flow).
- If market data is missing, verify `FINNHUB_API_KEY`.
- If analysis fails to start, verify `GROQ_API_KEY`.

