# Railway Build Commands & Configuration

This document provides explicit build commands and Railway configuration for each service.

## Backend Service

### Railway Configuration

**Settings → Source:**
- Root Directory: `backend`
- Dockerfile Path: `Dockerfile`

**Settings → Deploy:**
- Build Command: (leave empty - Railway uses Dockerfile)
- Start Command: (leave empty - Dockerfile handles this)

**OR use railway.json:**
- Rename `backend/railway-backend.json` to `backend/railway.json` (if using separate backend service)
- Or configure in Railway dashboard

### Manual Build Commands (for testing locally)

```bash
# Navigate to backend directory
cd backend

# Build Docker image locally
docker build -t financial-advisor-backend .

# Run locally
docker run -p 8080:8080 \
  -e DATABASE_URL=postgresql://user:pass@host:5432/dbname \
  -e LANGCHAIN4J_OLLAMA_BASE_URL=http://localhost:11434 \
  -e LANGCHAIN4J_OLLAMA_MODEL=llama3.1 \
  -e ALPHA_VANTAGE_API_KEY=your_key \
  financial-advisor-backend
```

### Railway Build Process

Railway will automatically:
1. Detect `backend/Dockerfile`
2. Build using multi-stage Docker build
3. Run `mvn clean package -DskipTests`
4. Create JAR file: `financialadvisor-0.0.1-SNAPSHOT.jar`
5. Copy to runtime image
6. Start with `java -jar app.jar`

### Environment Variables (Backend)

Set these in Railway dashboard → Variables:

```bash
# Database (auto-set by Railway PostgreSQL)
DATABASE_URL=postgresql://...

# Ollama Configuration
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Market Data
ALPHA_VANTAGE_API_KEY=your_key_here

# Server
PORT=8080

# CORS (optional)
CORS_ORIGINS=https://your-frontend.railway.app
```

---

## Ollama Service

### Railway Configuration

**Settings → Source:**
- Root Directory: `backend` (where Dockerfile.ollama is located)
- Dockerfile Path: `Dockerfile.ollama`

**Settings → Deploy:**
- Build Command: (leave empty)
- Start Command: (leave empty - Dockerfile handles this)

**OR use railway.json:**
- The `backend/railway.json` is already configured for Ollama
- Make sure Root Directory is set to `backend` in Railway

### Manual Build Commands (for testing locally)

```bash
# Navigate to backend directory
cd backend

# Build Ollama Docker image
docker build -f Dockerfile.ollama -t ollama-service .

# Run locally
docker run -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama-service

# In another terminal, pull model
docker exec -it <container_id> ollama pull llama3.1
```

### Railway Build Process

Railway will automatically:
1. Detect `backend/Dockerfile.ollama`
2. Pull `ollama/ollama:latest` image
3. Configure environment variables
4. Start `ollama serve`

### After Deployment - Pull Model

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login and link
railway login
railway link

# Connect to Ollama service
railway shell --service ollama

# Pull the model (this takes 5-10 minutes, downloads ~4GB)
ollama pull llama3.1

# Verify
ollama list
```

### Environment Variables (Ollama)

Usually no environment variables needed, but you can set:

```bash
OLLAMA_HOST=0.0.0.0:11434
OLLAMA_KEEP_ALIVE=24h
```

---

## Frontend Service

### Railway Configuration

**Settings → Source:**
- Root Directory: `frontend`

**Settings → Deploy:**
- Build Command: `npm install && npm run build`
- Start Command: `npx serve -s dist -p $PORT`
- Output Directory: `dist`

### Manual Build Commands (for testing locally)

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Test build locally
npx serve -s dist -p 3000
```

### Railway Build Process

Railway will automatically:
1. Run `npm install`
2. Run `npm run build`
3. Serve files from `dist` directory
4. Use port from `$PORT` environment variable

### Environment Variables (Frontend)

Set these in Railway dashboard → Variables:

```bash
# Backend API
VITE_API_BASE_URL=https://your-backend.railway.app/api

# WebSocket
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (auto-set by Railway)
PORT=3000
```

---

## Troubleshooting Build Failures

### Backend Build Fails

**Issue: Maven build fails**
```bash
# Check if pom.xml is correct
cd backend
mvn clean package -DskipTests

# If it works locally, check Railway logs
railway logs --service backend
```

**Issue: JAR file not found**
- The Dockerfile should find: `financialadvisor-0.0.1-SNAPSHOT.jar`
- Check `pom.xml` for correct artifact name
- Verify build completes: `mvn clean package -DskipTests`

**Issue: Port binding fails**
- Railway sets `PORT` automatically
- Make sure `application.yml` uses `${PORT:8080}`

### Ollama Build Fails

**Issue: Image pull fails**
- Check internet connection in Railway
- Try using specific version: `ollama/ollama:0.1.20`

**Issue: Model not found after deployment**
- You MUST pull the model after deployment
- Use `railway shell --service ollama` then `ollama pull llama3.1`

### Frontend Build Fails

**Issue: npm install fails**
```bash
# Check package.json
cd frontend
npm install

# If it works locally, check Railway logs
railway logs --service frontend
```

**Issue: Build command fails**
- Make sure `package.json` has correct scripts
- Check for missing dependencies
- Verify Node.js version (Railway uses latest LTS)

---

## Railway CLI Commands

```bash
# View all services
railway status

# View logs
railway logs

# View logs for specific service
railway logs --service backend
railway logs --service ollama
railway logs --service frontend

# Follow logs
railway logs --follow --service backend

# Set environment variable
railway variables set KEY=value --service backend

# View variables
railway variables --service backend

# SSH into service
railway shell --service ollama

# Redeploy service
railway up --service backend
```

---

## Quick Setup Checklist

### 1. Backend Service
- [ ] Root Directory: `backend`
- [ ] Dockerfile Path: `Dockerfile`
- [ ] Environment variables set
- [ ] Build succeeds
- [ ] Service is running

### 2. Ollama Service
- [ ] Root Directory: `backend`
- [ ] Dockerfile Path: `Dockerfile.ollama`
- [ ] Build succeeds
- [ ] Service is running
- [ ] Model pulled: `ollama pull llama3.1`

### 3. Frontend Service
- [ ] Root Directory: `frontend`
- [ ] Build Command: `npm install && npm run build`
- [ ] Start Command: `npx serve -s dist -p $PORT`
- [ ] Environment variables set
- [ ] Build succeeds
- [ ] Service is running

### 4. PostgreSQL Database
- [ ] Database created
- [ ] `DATABASE_URL` auto-set
- [ ] Backend can connect

---

## Alternative: Use Railway Nixpacks

If Docker builds fail, Railway can auto-detect and build:

### Backend (Nixpacks)
- Railway will detect `pom.xml`
- Auto-build with Maven
- Auto-detect Spring Boot
- Just set Root Directory to `backend`

### Frontend (Nixpacks)
- Railway will detect `package.json`
- Auto-build with npm
- Auto-detect Vite
- Just set Root Directory to `frontend`

**Note**: For Ollama, you MUST use Dockerfile (Nixpacks won't work).

---

## Build Time Estimates

- **Backend**: 3-5 minutes (Maven download + build)
- **Ollama**: 1-2 minutes (image pull)
- **Frontend**: 1-2 minutes (npm install + build)
- **Model Pull**: 5-10 minutes (first time only, ~4GB download)

---

For more details, see [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md)

