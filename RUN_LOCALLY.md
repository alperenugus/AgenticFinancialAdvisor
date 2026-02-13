# Running Backend Locally

## Quick Start

### Prerequisites

1. **Java 21** installed
2. **Maven** installed
3. **Ollama running locally** (optional - for full functionality)
   ```bash
   ollama serve
   ```

### Run with H2 Database (No PostgreSQL needed)

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

This uses:
- **H2 in-memory database** (no PostgreSQL setup needed)
- **Local Ollama** at `http://localhost:11434` (if running)
- **Port 8080**

### Run with PostgreSQL (Production-like)

1. **Start PostgreSQL locally** (Docker):
   ```bash
   docker run -d --name postgres-financial \
     -e POSTGRES_PASSWORD=postgres \
     -e POSTGRES_DB=financialadvisor \
     -p 5432:5432 \
     postgres:15
   ```

2. **Set environment variables:**
   ```bash
   export DATABASE_URL=jdbc:postgresql://localhost:5432/financialadvisor
   export SPRING_DATASOURCE_USERNAME=postgres
   export SPRING_DATASOURCE_PASSWORD=postgres
   export LANGCHAIN4J_OLLAMA_BASE_URL=http://localhost:11434
   export ALPHA_VANTAGE_API_KEY=your_key_here
   ```

3. **Run:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

---

## Environment Variables

### Required for Full Functionality

```bash
# Ollama (if running locally)
export LANGCHAIN4J_OLLAMA_BASE_URL=http://localhost:11434
export LANGCHAIN4J_OLLAMA_MODEL=llama3.1

# Market Data API
export ALPHA_VANTAGE_API_KEY=your_key_here

# Database (if using PostgreSQL)
export DATABASE_URL=jdbc:postgresql://localhost:5432/financialadvisor
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

### Optional

```bash
export CORS_ORIGINS=http://localhost:5173,http://localhost:3000
export NEWS_API_KEY=your_key_here
```

---

## Testing the Backend

### 1. Check Health

```bash
curl http://localhost:8080/api/advisor/status
```

Expected response:
```json
{
  "agents": {...},
  "status": "operational"
}
```

### 2. Test Analysis Endpoint

```bash
curl -X POST http://localhost:8080/api/advisor/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "query": "Should I buy Apple stock?",
    "sessionId": "test-session"
  }'
```

### 3. Access H2 Console (if using local profile)

Open browser: http://localhost:8080/h2-console

- **JDBC URL**: `jdbc:h2:mem:financialadvisor`
- **Username**: `sa`
- **Password**: (empty)

---

## Common Issues

### Port 8080 Already in Use

```bash
# Change port
export PORT=8081
# Or in application.yml: server.port: 8081
```

### Ollama Connection Failed

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# If not running, start it:
ollama serve
```

### Database Connection Failed

**If using PostgreSQL:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check connection
psql -h localhost -U postgres -d financialadvisor
```

**If using H2 (local profile):**
- H2 starts automatically, no setup needed

### Missing Dependencies

```bash
# Clean and rebuild
mvn clean install
```

---

## Debugging

### Enable Debug Logging

Edit `application-local.yml`:
```yaml
logging:
  level:
    com.agent.financialadvisor: DEBUG
    org.springframework: DEBUG
    org.hibernate: DEBUG
```

### View Logs

```bash
# Run with verbose logging
mvn spring-boot:run -Dspring-boot.run.profiles=local -X
```

### Check Application Properties

The app will use:
1. `application-local.yml` (if profile=local)
2. `application.yml` (default)

---

## Frontend Connection

Once backend is running locally:

1. **Start frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

2. **Frontend will connect to:**
   - API: `http://localhost:8080/api`
   - WebSocket: `http://localhost:8080/ws`

3. **Open browser:**
   - http://localhost:5173

---

## Production vs Local

| Feature | Local (H2) | Production (PostgreSQL) |
|---------|------------|------------------------|
| Database | H2 in-memory | PostgreSQL |
| Data Persistence | Lost on restart | Persistent |
| Setup | None | Docker/PostgreSQL |
| Good for | Testing | Production |

---

## Summary

**Easiest way to run locally:**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

This uses H2 database (no setup needed) and connects to local Ollama if running.

**For production-like testing:**
- Use PostgreSQL
- Set all environment variables
- Run without `-Dspring-boot.run.profiles=local`

