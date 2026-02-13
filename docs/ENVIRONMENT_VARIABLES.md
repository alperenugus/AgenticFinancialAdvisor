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

# Google OAuth2 Configuration (REQUIRED)
# See: docs/GOOGLE_AUTH_SETUP.md for setup instructions
GOOGLE_CLIENT_ID=your_google_client_id_here
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google

# JWT Configuration (REQUIRED)
# Generate a secure random string (minimum 32 characters)
# Example: openssl rand -base64 32
JWT_SECRET=your-secure-random-secret-key-minimum-32-characters-long
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

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
# CORS Origins (REQUIRED if frontend on different domain)
# Must include your frontend URL for OAuth2 redirects
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173

# Frontend Redirect URL (for OAuth2 callback)
# Default: http://localhost:5173 (local) or your frontend URL (production)
FRONTEND_REDIRECT_URL=https://your-frontend.railway.app

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
- [ ] `GOOGLE_CLIENT_ID` - **REQUIRED** - Get from Google Cloud Console
- [ ] `GOOGLE_CLIENT_SECRET` - **REQUIRED** - Get from Google Cloud Console
- [ ] `GOOGLE_REDIRECT_URI` - **REQUIRED** - `https://your-backend.railway.app/login/oauth2/code/google`
- [ ] `JWT_SECRET` - **REQUIRED** - Generate secure random 32+ character string
- [ ] `JWT_EXPIRATION` - Optional - Default: 86400000 (24 hours)
- [ ] `GROQ_API_KEY` - **REQUIRED** - Get from https://console.groq.com/
- [ ] `ALPHA_VANTAGE_API_KEY` - **REQUIRED** - Get from https://www.alphavantage.co/support/#api-key
- [ ] `CORS_ORIGINS` - **REQUIRED** - Include frontend URL for OAuth2
- [ ] `FRONTEND_REDIRECT_URL` - Optional - Frontend URL for OAuth2 callback
- [ ] `PORT=8080` - Usually auto-set
- [ ] (Optional) `GROQ_ORCHESTRATOR_MODEL` - Default: llama-3.3-70b-versatile
- [ ] (Optional) `GROQ_TOOL_AGENT_MODEL` - Default: llama-3.1-8b-instant
- [ ] (Optional) `NEWS_API_KEY` - If using NewsAPI

### Frontend Service Variables

- [ ] `VITE_API_BASE_URL` - Backend URL + `/api` (e.g., `https://your-backend.railway.app/api`)
- [ ] `VITE_WS_URL` - Backend URL + `/ws` (e.g., `https://your-backend.railway.app/ws`)
- [ ] `PORT=3000` - Usually auto-set

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
railway variables set GOOGLE_CLIENT_ID=your_client_id --service backend
railway variables set GOOGLE_CLIENT_SECRET=your_client_secret --service backend
railway variables set GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google --service backend
railway variables set JWT_SECRET=$(openssl rand -base64 32) --service backend
railway variables set GROQ_API_KEY=your_key --service backend
railway variables set ALPHA_VANTAGE_API_KEY=your_key --service backend
railway variables set CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173 --service backend

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
GOOGLE_CLIENT_ID=123456789-abcdefghijklmnop.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-abcdefghijklmnopqrstuvwxyz
GOOGLE_REDIRECT_URI=https://backend-production-xxxx.up.railway.app/login/oauth2/code/google
JWT_SECRET=your-secure-random-32-character-secret-key-here
GROQ_API_KEY=gsk_abc123xyz789
ALPHA_VANTAGE_API_KEY=ABC123XYZ789
CORS_ORIGINS=https://frontend-production-xxxx.up.railway.app,http://localhost:5173
FRONTEND_REDIRECT_URL=https://frontend-production-xxxx.up.railway.app
PORT=8080
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

### Google OAuth2 Credentials (Required)

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project or select existing one
3. Enable Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Configure:
   - Application type: Web application
   - Authorized redirect URIs: `https://your-backend.railway.app/login/oauth2/code/google`
6. Copy Client ID and Client Secret
7. Set as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`

See [Google Auth Setup Guide](../GOOGLE_AUTH_SETUP.md) for detailed instructions.

### Groq API Key (Required)

1. Go to https://console.groq.com/
2. Sign up or log in
3. Create an API key
4. Set as `GROQ_API_KEY`

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

### JWT Secret (Required)

Generate a secure random secret:

```bash
# Using OpenSSL
openssl rand -base64 32

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Or using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

Set as `JWT_SECRET` (minimum 32 characters).

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

- Forgetting to set `GOOGLE_CLIENT_ID` or `GOOGLE_CLIENT_SECRET` → Authentication won't work
- Forgetting to set `JWT_SECRET` → Authentication will fail
- Forgetting to set `GROQ_API_KEY` → LLM agents won't work
- Forgetting to set `ALPHA_VANTAGE_API_KEY` → Market data won't work
- Forgetting to set `VITE_API_BASE_URL` → Frontend can't connect to backend
- Forgetting to set `CORS_ORIGINS` → OAuth2 redirects will fail
- Using internal URL for frontend → `ERR_NAME_NOT_RESOLVED` error

---

## Summary

### Minimum Required (Backend)

1. `DATABASE_URL` - Auto-set by PostgreSQL
2. `GOOGLE_CLIENT_ID` - From Google Cloud Console
3. `GOOGLE_CLIENT_SECRET` - From Google Cloud Console
4. `GOOGLE_REDIRECT_URI` - `https://your-backend.railway.app/login/oauth2/code/google`
5. `JWT_SECRET` - Generate secure random 32+ character string
6. `GROQ_API_KEY` - Get from https://console.groq.com/
7. `ALPHA_VANTAGE_API_KEY` - Get free key
8. `CORS_ORIGINS` - Include frontend URL

### Minimum Required (Frontend)

1. `VITE_API_BASE_URL` - Backend URL + `/api`
2. `VITE_WS_URL` - Backend URL + `/ws`

---

**Quick Reference**: Set these 8 backend + 2 frontend variables and you're good to go!

