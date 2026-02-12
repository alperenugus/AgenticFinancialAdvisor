# Railway Project Structure

## Overview

You create **ONE Railway project** and then add **multiple services** within that project.

---

## Step-by-Step Setup

### Step 1: Create ONE Railway Project

1. Go to [railway.app](https://railway.app)
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Choose your repository: `alperenugus/AgenticFinancialAdvisor`
5. Railway creates **ONE project** with your repo connected

---

### Step 2: Add Services Within That Project

Within your **single Railway project**, you'll create **4 services**:

#### Service 1: PostgreSQL Database
- Click **"New"** → **"Database"** → **"PostgreSQL"**
- Railway automatically creates and manages this
- **No configuration needed** - Railway sets `DATABASE_URL` automatically

#### Service 2: Ollama Service
- Click **"New"** → **"Empty Service"** (or "GitHub Repo" and select your repo)
- Name it: `ollama` or `ollama-service`
- Configure:
  - **Root Directory**: `backend`
  - **Dockerfile Path**: `Dockerfile.ollama`
  - **Builder**: `Dockerfile`

#### Service 3: Backend Service
- Click **"New"** → **"GitHub Repo"** (select your repo again)
- Name it: `backend` or `financial-advisor-backend`
- Configure:
  - **Root Directory**: `backend`
  - **Dockerfile Path**: `Dockerfile`
  - **Builder**: `Dockerfile`

#### Service 4: Frontend Service
- Click **"New"** → **"GitHub Repo"** (select your repo again)
- Name it: `frontend` or `financial-advisor-frontend`
- Configure:
  - **Root Directory**: `frontend`
  - **Build Command**: `npm install && npm run build`
  - **Start Command**: `npx serve -s dist -p $PORT`
  - **Output Directory**: `dist`

---

## Visual Structure

```
Railway Project: "AgenticFinancialAdvisor"
│
├── 📊 PostgreSQL Database (Service 1)
│   └── Auto-managed by Railway
│   └── Provides: DATABASE_URL (auto-set)
│
├── 🤖 Ollama Service (Service 2)
│   └── Root: backend
│   └── Dockerfile: Dockerfile.ollama
│   └── Port: 11434
│
├── ⚙️ Backend Service (Service 3)
│   └── Root: backend
│   └── Dockerfile: Dockerfile
│   └── Port: 8080
│   └── Uses: DATABASE_URL, LANGCHAIN4J_OLLAMA_BASE_URL
│
└── 🎨 Frontend Service (Service 4)
    └── Root: frontend
    └── Build: npm install && npm run build
    └── Start: npx serve -s dist -p $PORT
    └── Uses: VITE_API_BASE_URL, VITE_WS_URL
```

---

## Why This Structure?

### ✅ Benefits of One Project with Multiple Services:

1. **Shared Environment Variables**: Services in the same project can share variables
2. **Easy Networking**: Services can communicate via internal URLs
3. **Unified Billing**: All services under one project
4. **Simple Management**: One dashboard for all services
5. **Auto-Discovery**: PostgreSQL `DATABASE_URL` automatically available to backend

### ❌ Don't Create Separate Projects:

- Separate projects = separate billing
- No automatic variable sharing
- More complex networking
- Harder to manage

---

## Complete Setup Checklist

### 1. Create Project
- [ ] New Project → Deploy from GitHub
- [ ] Select: `alperenugus/AgenticFinancialAdvisor`

### 2. Add PostgreSQL
- [ ] New → Database → PostgreSQL
- [ ] Wait for creation (auto-sets DATABASE_URL)

### 3. Add Ollama Service
- [ ] New → Empty Service (or GitHub Repo)
- [ ] Name: `ollama`
- [ ] Root Directory: `backend`
- [ ] Dockerfile Path: `Dockerfile.ollama`
- [ ] Builder: `Dockerfile`
- [ ] After deploy: `railway shell --service ollama` → `ollama pull llama3.1`

### 4. Add Backend Service
- [ ] New → GitHub Repo
- [ ] Name: `backend`
- [ ] Root Directory: `backend`
- [ ] Dockerfile Path: `Dockerfile`
- [ ] Builder: `Dockerfile`
- [ ] Set Environment Variables:
  - `LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama.railway.app`
  - `LANGCHAIN4J_OLLAMA_MODEL=llama3.1`
  - `ALPHA_VANTAGE_API_KEY=your_key`
  - `DATABASE_URL` (auto-set by PostgreSQL)

### 5. Add Frontend Service
- [ ] New → GitHub Repo
- [ ] Name: `frontend`
- [ ] Root Directory: `frontend`
- [ ] Build Command: `npm install && npm run build`
- [ ] Start Command: `npx serve -s dist -p $PORT`
- [ ] Set Environment Variables:
  - `VITE_API_BASE_URL=https://your-backend.railway.app/api`
  - `VITE_WS_URL=https://your-backend.railway.app/ws`

---

## Service Communication

### How Services Connect:

1. **Backend → PostgreSQL**: 
   - Uses `DATABASE_URL` (auto-set by Railway)
   - Internal connection (fast, secure)

2. **Backend → Ollama**:
   - Uses `LANGCHAIN4J_OLLAMA_BASE_URL`
   - Set to Ollama service's public URL
   - Example: `https://ollama-production-xxxx.up.railway.app`

3. **Frontend → Backend**:
   - Uses `VITE_API_BASE_URL` for REST API
   - Uses `VITE_WS_URL` for WebSocket
   - Set to Backend service's public URL
   - Example: `https://backend-production-xxxx.up.railway.app`

---

## Railway Dashboard View

After setup, your Railway dashboard will show:

```
Project: AgenticFinancialAdvisor
├── 🟢 PostgreSQL (Running)
├── 🟢 ollama (Running)
├── 🟢 backend (Running)
└── 🟢 frontend (Running)
```

Each service has:
- Its own logs
- Its own environment variables
- Its own public URL
- Its own deployment history

---

## Cost Structure

All services are billed under **ONE project**:
- Railway Free Tier: $5 credit/month
- All 4 services share this credit
- Estimated: ~$6-9/month total (may exceed free tier)

---

## Quick Start Commands

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to your project (select the project when prompted)
railway link

# View all services
railway status

# View logs for specific service
railway logs --service backend
railway logs --service ollama
railway logs --service frontend

# Set environment variable
railway variables set KEY=value --service backend

# SSH into service
railway shell --service ollama
```

---

## Summary

**Answer: Create ONE Railway project, then add 4 services within it.**

- ✅ One project
- ✅ Four services (PostgreSQL, Ollama, Backend, Frontend)
- ✅ All services in the same project
- ✅ Easy management and communication

---

For detailed deployment steps, see [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md)

