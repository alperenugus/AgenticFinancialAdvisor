# Complete Railway Deployment Guide

This is the **complete guide** for deploying the Agentic Financial Advisor on Railway.

## Table of Contents

1. [Project Structure](#project-structure)
2. [Step-by-Step Deployment](#step-by-step-deployment)
3. [Environment Variables](#environment-variables)
4. [Build Configuration](#build-configuration)
5. [Troubleshooting](#troubleshooting)

---

## Project Structure

You create **ONE Railway project** and add **3 services** within it:

```
Railway Project: "AgenticFinancialAdvisor"
├── 📊 PostgreSQL Database
├── ⚙️ Backend Service
└── 🎨 Frontend Service
```

**Note:** The system uses **Groq API** for LLM inference (no local Ollama service needed).

---

## Step-by-Step Deployment

### Step 1: Create Railway Project

1. Go to [railway.app](https://railway.app)
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select: `alperenugus/AgenticFinancialAdvisor`

### Step 2: Deploy PostgreSQL

1. **New** → **Database** → **PostgreSQL**
2. Railway auto-sets `DATABASE_URL` (no config needed)

### Step 3: Deploy Backend Service

1. **New** → **GitHub Repo**
2. Name: `backend`
3. **Settings** → **Source**:
   - Root Directory: `backend`
4. **Settings** → **Deploy**:
   - Builder: `Dockerfile`
5. **Settings** → **Variables** (see [Environment Variables](#environment-variables))

### Step 4: Deploy Frontend Service

1. **New** → **GitHub Repo**
2. Name: `frontend`
3. **Settings** → **Source**:
   - Root Directory: `frontend`
4. **Settings** → **Deploy**:
   - Build Command: `npm install && npm run build`
   - Start Command: `npx serve -s dist -p $PORT`
5. **Settings** → **Variables** (see [Environment Variables](#environment-variables))

---

## Environment Variables

### Backend Service

```bash
# Groq API (REQUIRED - Get from https://console.groq.com/)
GROQ_API_KEY=your_groq_api_key_here

# Market Data (REQUIRED - Get from https://finnhub.io/)
FINNHUB_API_KEY=your_finnhub_api_key_here

# Google OAuth2 (REQUIRED)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google

# JWT (REQUIRED)
JWT_SECRET=your-secure-random-secret-key-minimum-32-characters

# CORS (REQUIRED)
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173

# Database (auto-set by PostgreSQL)
DATABASE_URL=postgresql://...

# Port (auto-set)
PORT=8080
```

### Frontend Service

```bash
# Backend URLs (REQUIRED)
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (auto-set)
PORT=3000
```

**See:** [../../ENVIRONMENT_VARIABLES.md](../ENVIRONMENT_VARIABLES.md) for complete details.

---

## Build Configuration

### Backend

- **Root Directory**: `backend`
- **Builder**: `Dockerfile`
- **Dockerfile**: `backend/Dockerfile`
- Uses `railway.toml` for health check configuration

### Frontend

- **Root Directory**: `frontend`
- **Build Command**: `npm install && npm run build`
- **Start Command**: `npx serve -s dist -p $PORT`

**Note:** Railway automatically detects and builds the backend using the Dockerfile.

---

## Getting API Keys

### Groq API Key
1. Sign up at [Groq Console](https://console.groq.com/)
2. Create an API key
3. Set as `GROQ_API_KEY` environment variable

### Finnhub API Key
1. Sign up at [Finnhub](https://finnhub.io/)
2. Get your free API key
3. Set as `FINNHUB_API_KEY` environment variable

### Google OAuth2
1. Follow [Google Auth Setup Guide](../GOOGLE_AUTH_SETUP.md)
2. Set `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `GOOGLE_REDIRECT_URI`

---

## Troubleshooting

### Build Issues

- **Build fails**: Check Railway logs for specific error messages
- **Dockerfile issues**: Ensure Dockerfile is in the `backend` directory
- **Dependencies**: Verify all dependencies are correctly specified in `pom.xml`

### Health Check Failures

- **Backend health check**: See [../troubleshooting/HEALTHCHECK_TROUBLESHOOTING.md](../troubleshooting/HEALTHCHECK_TROUBLESHOOTING.md)
- **Health check timeout fixes**: See [../troubleshooting/RAILWAY_HEALTHCHECK_FIX.md](../troubleshooting/RAILWAY_HEALTHCHECK_FIX.md)

### Access Issues

- **Authentication issues**: See [../GOOGLE_AUTH_TROUBLESHOOTING.md](../GOOGLE_AUTH_TROUBLESHOOTING.md)
- **UI issues**: See [../HOW_TO_CHAT.md](../HOW_TO_CHAT.md)
- **CORS issues**: See [../troubleshooting/CORS_FIX.md](../troubleshooting/CORS_FIX.md)


---

## Quick Reference

### Service URLs

After deployment, get URLs from:
- Railway Dashboard → Service → **Settings** → **Networking** → **Public Domain**

### Railway CLI

```bash
railway login
railway link
railway logs --service backend
railway variables set KEY=value --service backend
railway variables --service backend  # List all variables
```

---

## Next Steps

1. ✅ Deploy PostgreSQL database
2. ✅ Deploy backend service
3. ✅ Deploy frontend service
4. ✅ Set all environment variables (Groq, Finnhub, Google OAuth2, JWT)
5. ✅ Test authentication flow
6. ✅ Test API endpoints
7. 🎉 Start using the app!

For troubleshooting, see [RAILWAY_TROUBLESHOOTING.md](./RAILWAY_TROUBLESHOOTING.md)

