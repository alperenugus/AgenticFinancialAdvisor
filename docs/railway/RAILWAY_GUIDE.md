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

You create **ONE Railway project** and add **4 services** within it:

```
Railway Project: "AgenticFinancialAdvisor"
├── 📊 PostgreSQL Database
├── 🤖 Ollama Service
├── ⚙️ Backend Service
└── 🎨 Frontend Service
```

**See:** [RAILWAY_STRUCTURE.md](./RAILWAY_STRUCTURE.md) for detailed structure explanation.

---

## Step-by-Step Deployment

### Step 1: Create Railway Project

1. Go to [railway.app](https://railway.app)
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select: `alperenugus/AgenticFinancialAdvisor`

### Step 2: Deploy PostgreSQL

1. **New** → **Database** → **PostgreSQL**
2. Railway auto-sets `DATABASE_URL` (no config needed)

### Step 3: Deploy Ollama Service

1. **New** → **Empty Service** (or GitHub Repo)
2. Name: `ollama`
3. **Settings** → **Source**:
   - Root Directory: `ollama`
4. **Settings** → **Deploy**:
   - Builder: `Dockerfile`
   - Dockerfile Path: `Dockerfile`
5. **Settings** → **Volumes**:
   - Add Volume: Mount Path `/root/.ollama`, Size `20GB`
6. After deployment: Model auto-pulls (see [Ollama Setup](#ollama-setup))

### Step 4: Deploy Backend Service

1. **New** → **GitHub Repo**
2. Name: `backend`
3. **Settings** → **Source**:
   - Root Directory: `backend`
4. **Settings** → **Deploy**:
   - Builder: `Dockerfile`
5. **Settings** → **Variables** (see [Environment Variables](#environment-variables))

### Step 5: Deploy Frontend Service

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
# Ollama (REQUIRED)
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Market Data (REQUIRED)
ALPHA_VANTAGE_API_KEY=your_key_here

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

### Ollama

- **Root Directory**: `ollama`
- **Builder**: `Dockerfile`
- **Dockerfile**: `ollama/Dockerfile`
- Auto-pulls model on startup

### Frontend

- **Root Directory**: `frontend`
- **Build Command**: `npm install && npm run build`
- **Start Command**: `npx serve -s dist -p $PORT`

**See:** [RAILWAY_BUILD_COMMANDS.md](./RAILWAY_BUILD_COMMANDS.md) for detailed build info.

---

## Ollama Setup

### Automatic Model Pulling

The Ollama service automatically pulls `llama3.1` on startup. No manual steps needed!

### Railway Volume (Recommended)

1. Ollama service → **Settings** → **Volumes**
2. Add volume:
   - Mount Path: `/root/.ollama`
   - Size: `20GB`
3. Models persist across restarts

**See:** [../troubleshooting/OLLAMA_DISK_SPACE.md](../troubleshooting/OLLAMA_DISK_SPACE.md) for disk space issues.

---

## Troubleshooting

### Build Issues

- **Nixpacks error**: See [RAILWAY_FIX_NIXPACKS.md](./RAILWAY_FIX_NIXPACKS.md)
- **Build fails**: See [RAILWAY_BUILD_COMMANDS.md](./RAILWAY_BUILD_COMMANDS.md)

### Health Check Failures

- **Backend health check**: See [../troubleshooting/HEALTHCHECK_TROUBLESHOOTING.md](../troubleshooting/HEALTHCHECK_TROUBLESHOOTING.md)
- **Health check timeout fixes**: See [../troubleshooting/RAILWAY_HEALTHCHECK_FIX.md](../troubleshooting/RAILWAY_HEALTHCHECK_FIX.md)
- **Ollama 502 error**: See [../troubleshooting/OLLAMA_DISK_SPACE.md](../troubleshooting/OLLAMA_DISK_SPACE.md)

### Access Issues

- **Ollama not accessible**: See [../troubleshooting/OLLAMA_ACCESS_FIX.md](../troubleshooting/OLLAMA_ACCESS_FIX.md)
- **UI issues**: See [../HOW_TO_CHAT.md](../HOW_TO_CHAT.md)

### UI Configuration

- **Railway UI changes**: See [RAILWAY_UI_GUIDE.md](./RAILWAY_UI_GUIDE.md)

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
railway shell --service ollama
railway variables set KEY=value --service backend
```

---

## Next Steps

1. ✅ Deploy all services
2. ✅ Set environment variables
3. ✅ Add Ollama volume
4. ✅ Test endpoints
5. 🎉 Start using the app!

For detailed step-by-step instructions, see [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md)

