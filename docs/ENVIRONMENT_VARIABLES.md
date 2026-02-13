# Environment Variables - Complete Guide

This document lists all environment variables needed for Railway deployment.

---

## Backend Service

### Required Variables

```bash
# Database (Auto-set by Railway PostgreSQL - verify it exists)
DATABASE_URL=postgresql://user:password@host:5432/dbname
# OR manually set:
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Groq API Configuration (REQUIRED)
# Get your API key from: https://console.groq.com/
GROQ_API_KEY=your_groq_api_key_here

# Optional: Override default models
# Orchestrator model (high-level thinking) - default: llama-3.3-70b-versatile
GROQ_ORCHESTRATOR_MODEL=llama-3.3-70b-versatile
GROQ_ORCHESTRATOR_TEMPERATURE=0.7
GROQ_ORCHESTRATOR_TIMEOUT_SECONDS=60

# Tool Agent model (fast function calling) - default: llama-3.1-8b-instant
GROQ_TOOL_AGENT_MODEL=llama-3.1-8b-instant
GROQ_TOOL_AGENT_TEMPERATURE=0.3
GROQ_TOOL_AGENT_TIMEOUT_SECONDS=30

# Market Data API (REQUIRED - get free key from https://www.alphavantage.co/support/#api-key)
ALPHA_VANTAGE_API_KEY=your_alpha_vantage_api_key_here

# Server Port (Auto-set by Railway - usually don't need to set)
PORT=8080
```

### Optional Variables

```bash
# CORS Origins (if frontend on different domain)
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173

# News API (optional - get free key from https://newsapi.org/)
NEWS_API_KEY=your_news_api_key_here
```

---

## Frontend Service

### Required Variables

```bash
# Backend API URL (REQUIRED - MUST be external/public URL!)
# ⚠️ IMPORTANT: Frontend runs in browser, so it CANNOT use internal URLs
# ❌ WRONG: http://backend.railway.internal:8080/api
# ✅ CORRECT: https://your-backend.railway.app/api
VITE_API_BASE_URL=https://your-backend.railway.app/api

# WebSocket URL (REQUIRED - MUST be external/public URL!)
# ⚠️ IMPORTANT: Frontend runs in browser, so it CANNOT use internal URLs
# ❌ WRONG: http://backend.railway.internal:8080/ws
# ✅ CORRECT: https://your-backend.railway.app/ws
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (Auto-set by Railway - usually don't need to set)
PORT=3000
```

### ⚠️ Critical: Frontend Must Use External URLs

**The frontend runs in the user's browser**, which means:
- ✅ **MUST use external/public URLs** (e.g., `https://backend.railway.app`)
- ❌ **CANNOT use internal URLs** (e.g., `http://backend.railway.internal:8080`)

**Why?** The browser doesn't have access to Railway's internal network. Internal URLs only work for service-to-service communication within Railway.

---

## PostgreSQL Database

### Auto-Configured by Railway

```bash
# Railway automatically sets DATABASE_URL
# No manual configuration needed
# Just verify it exists in backend service variables
```

---

## Quick Setup Checklist

### Backend Service Variables

- [ ] `DATABASE_URL` - Auto-set by PostgreSQL (verify it's there)
- [ ] `GROQ_API_KEY` - **REQUIRED** - Get from https://console.groq.com/
- [ ] `ALPHA_VANTAGE_API_KEY` - Get from https://www.alphavantage.co/support/#api-key
- [ ] `PORT=8080` - Usually auto-set
- [ ] (Optional) `GROQ_ORCHESTRATOR_MODEL` - Default: llama-3.3-70b-versatile
- [ ] (Optional) `GROQ_TOOL_AGENT_MODEL` - Default: llama-3.1-8b-instant
- [ ] (Optional) `CORS_ORIGINS` - If frontend on different domain
- [ ] (Optional) `NEWS_API_KEY` - If using NewsAPI

### Frontend Service Variables

- [ ] `VITE_API_BASE_URL` - Backend URL + `/api`
- [ ] `VITE_WS_URL` - Backend URL + `/ws`
- [ ] `PORT=3000` - Usually auto-set

### Ollama Service Variables

- [ ] Usually none needed
- [ ] (Optional) `OLLAMA_HOST=0.0.0.0:11434`

---

## How to Set in Railway

### Method 1: Railway Dashboard (Recommended)

1. Go to your service (Backend, Frontend, etc.)
2. Click **Settings** tab
3. Click **Variables** section
4. Click **"New Variable"** or **"Add Variable"**
5. Enter:
   - **Name**: `LANGCHAIN4J_OLLAMA_BASE_URL`
   - **Value**: `https://your-ollama-service.railway.app`
6. Click **Save**
7. Repeat for each variable

### Method 2: Railway CLI

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# Set variables for backend
railway variables set LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama.railway.app --service backend
railway variables set LANGCHAIN4J_OLLAMA_MODEL=llama3.1 --service backend
railway variables set ALPHA_VANTAGE_API_KEY=your_key --service backend

# Set variables for frontend
railway variables set VITE_API_BASE_URL=https://your-backend.railway.app/api --service frontend
railway variables set VITE_WS_URL=https://your-backend.railway.app/ws --service frontend
```

---

## Example Values

### Backend Service

```bash
# Replace these with your actual URLs and keys:

DATABASE_URL=postgresql://postgres:password@containers-us-west-xxx.railway.app:5432/railway
LANGCHAIN4J_OLLAMA_BASE_URL=https://ollama-production-xxxx.up.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7
ALPHA_VANTAGE_API_KEY=ABC123XYZ789
PORT=8080
CORS_ORIGINS=https://frontend-production-xxxx.up.railway.app
```

### Frontend Service

```bash
# Replace with your actual backend URL:

VITE_API_BASE_URL=https://backend-production-xxxx.up.railway.app/api
VITE_WS_URL=https://backend-production-xxxx.up.railway.app/ws
PORT=3000
```

---

## Getting API Keys

### Alpha Vantage API Key (Required)

1. Go to https://www.alphavantage.co/support/#api-key
2. Fill out the form
3. Get your free API key (500 calls/day, 5 calls/minute)
4. Set as `ALPHA_VANTAGE_API_KEY`

### NewsAPI Key (Optional)

1. Go to https://newsapi.org/
2. Sign up for free account
3. Get your API key (100 requests/day free)
4. Set as `NEWS_API_KEY`

---

## Finding Service URLs

### In Railway Dashboard

1. Go to your service (Backend, Ollama, Frontend)
2. Click **Settings** → **Networking**
3. Copy the **Public Domain** URL
4. Use this URL in other services' environment variables

### Example URLs

- **Ollama**: `https://ollama-production-xxxx.up.railway.app`
- **Backend**: `https://backend-production-xxxx.up.railway.app`
- **Frontend**: `https://frontend-production-xxxx.up.railway.app`

---

## Verification

### Check Variables Are Set

```bash
# View all variables for a service
railway variables --service backend
railway variables --service frontend
```

### Test Backend

```bash
# Test if backend is accessible
curl https://your-backend.railway.app/api/advisor/status
```

### Test Frontend

1. Open frontend URL in browser
2. Check browser console for API connection errors
3. Verify it can connect to backend

---

## Common Mistakes

### ❌ Wrong URLs

```bash
# Wrong - missing protocol
LANGCHAIN4J_OLLAMA_BASE_URL=ollama-production-xxxx.up.railway.app

# Correct
LANGCHAIN4J_OLLAMA_BASE_URL=https://ollama-production-xxxx.up.railway.app
```

### ❌ Frontend Using Internal URLs (CRITICAL ERROR!)

```bash
# ❌ WRONG - Frontend CANNOT use internal URLs!
VITE_API_BASE_URL=http://backend.railway.internal:8080/api

# ✅ CORRECT - Frontend MUST use external URLs
VITE_API_BASE_URL=https://backend.railway.app/api
```

**Error you'll see:** `net::ERR_NAME_NOT_RESOLVED` or `Network Error`

**Why?** The browser doesn't have access to Railway's internal network. Only services within Railway can use `.railway.internal` URLs.

### ❌ Wrong Paths

```bash
# Wrong - missing /api
VITE_API_BASE_URL=https://backend.railway.app

# Correct
VITE_API_BASE_URL=https://backend.railway.app/api
```

### ❌ Missing Variables

- Forgetting to set `LANGCHAIN4J_OLLAMA_BASE_URL` → Backend can't connect to Ollama
- Forgetting to set `ALPHA_VANTAGE_API_KEY` → Market data won't work
- Forgetting to set `VITE_API_BASE_URL` → Frontend can't connect to backend
- Using internal URL for frontend → `ERR_NAME_NOT_RESOLVED` error

---

## Summary

### Minimum Required (Backend)

1. `DATABASE_URL` - Auto-set by PostgreSQL
2. `LANGCHAIN4J_OLLAMA_BASE_URL` - Your Ollama service URL
3. `LANGCHAIN4J_OLLAMA_MODEL=llama3.1`
4. `ALPHA_VANTAGE_API_KEY` - Get free key

### Minimum Required (Frontend)

1. `VITE_API_BASE_URL` - Backend URL + `/api`
2. `VITE_WS_URL` - Backend URL + `/ws`

---

**Quick Reference**: Set these 4-6 variables and you're good to go!

