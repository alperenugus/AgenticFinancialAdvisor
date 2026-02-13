# How to Chat with Your Financial Advisor Agent

## Important: Ollama URL Issue

Your Ollama URL shows port `:8080`, but Ollama typically runs on port `11434`. 

**Your Ollama URL should be:**
```
https://agenticfinancialadvisorollama-production.up.railway.app
```
(No port number - Railway handles this automatically)

If Railway shows `:8080`, that might be a display issue. The actual Ollama service should be accessible without the port.

---

## Architecture Overview

You can't chat directly with Ollama. You need **3 services** working together:

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Frontend   │─────▶│   Backend   │─────▶│   Ollama    │
│  (React)    │      │ (Spring Boot)│      │   (LLM)     │
│  Chat UI    │◀─────│    API      │◀─────│             │
└─────────────┘      └─────────────┘      └─────────────┘
     ↑                      ↑
     │                      │
     └──────────────────────┘
        WebSocket Connection
```

1. **Frontend** - The chat interface you see in your browser
2. **Backend** - Processes requests, coordinates agents, connects to Ollama
3. **Ollama** - The LLM that generates responses (already deployed ✅)

---

## Step-by-Step: Get Chat Working

### Step 1: Verify Ollama is Working

First, test if Ollama is accessible:

```bash
# Test Ollama API
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

**Expected response:** List of models (should include `llama3.1` if model was pulled)

**If this fails:**
- Check Railway dashboard - is Ollama service running?
- Verify the URL (remove `:8080` if present)
- Check if model was pulled: `railway shell --service ollama` → `ollama list`

---

### Step 2: Deploy Backend Service

The backend connects to Ollama and provides the API.

#### 2.1 Create Backend Service in Railway

1. Go to Railway dashboard
2. Click **"New"** → **"GitHub Repo"**
3. Select your repository: `alperenugus/AgenticFinancialAdvisor`
4. Name it: `backend`

#### 2.2 Configure Backend Service

1. Click on **Backend** service
2. **Settings** → **Source**:
   - **Root Directory**: `backend`
3. **Settings** → **Deploy**:
   - **Builder**: `Dockerfile`
   - **Dockerfile Path**: `Dockerfile`

#### 2.3 Set Environment Variables

Go to **Settings** → **Variables** and add:

```bash
# Ollama Configuration (use your Ollama URL WITHOUT :8080)
LANGCHAIN4J_OLLAMA_BASE_URL=https://agenticfinancialadvisorollama-production.up.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Database (should be auto-set by PostgreSQL)
DATABASE_URL=postgresql://...  # Verify this exists

# Market Data API
ALPHA_VANTAGE_API_KEY=your_key_here

# Port (auto-set by Railway)
PORT=8080
```

**Important:** Use your Ollama URL **without** `:8080`:
- ❌ Wrong: `https://...railway.app:8080`
- ✅ Correct: `https://...railway.app`

#### 2.4 Wait for Backend to Deploy

- Build takes 3-5 minutes
- Health check takes 3-5 minutes
- Once healthy, copy the **Backend Public URL**

---

### Step 3: Deploy Frontend Service

The frontend is the chat interface you'll use.

#### 3.1 Create Frontend Service in Railway

1. Go to Railway dashboard
2. Click **"New"** → **"GitHub Repo"**
3. Select your repository again
4. Name it: `frontend`

#### 3.2 Configure Frontend Service

1. Click on **Frontend** service
2. **Settings** → **Source**:
   - **Root Directory**: `frontend`
3. **Settings** → **Deploy**:
   - **Build Command**: `npm install && npm run build`
   - **Start Command**: `npx serve -s dist -p $PORT`
   - **Output Directory**: `dist`

#### 3.3 Set Environment Variables

Go to **Settings** → **Variables** and add:

```bash
# Backend API URL (use your Backend service URL from Step 2.4)
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (auto-set by Railway)
PORT=3000
```

**Replace** `your-backend.railway.app` with your actual Backend service URL.

#### 3.4 Wait for Frontend to Deploy

- Build takes 2-3 minutes
- Once deployed, copy the **Frontend Public URL**

---

### Step 4: Chat with Your Agent! 🎉

1. **Open your Frontend URL** in a browser
   - Example: `https://frontend-production-xxxx.up.railway.app`

2. **You should see:**
   - Chat interface
   - Tabs: Chat, Portfolio, Recommendations, Profile

3. **Start chatting:**
   - Type a question like: "Should I buy Apple stock?"
   - The agent will:
     - Connect to Backend
     - Backend connects to Ollama
     - Get market data from Alpha Vantage
     - Generate a response
     - Display it in the chat

---

## Quick Checklist

### Services Needed:
- [x] ✅ Ollama Service (already deployed)
- [ ] ⏳ Backend Service (deploy now)
- [ ] ⏳ Frontend Service (deploy after backend)
- [ ] ⏳ PostgreSQL Database (should be auto-created)

### Environment Variables:
- [ ] Backend: `LANGCHAIN4J_OLLAMA_BASE_URL` (your Ollama URL)
- [ ] Backend: `ALPHA_VANTAGE_API_KEY`
- [ ] Frontend: `VITE_API_BASE_URL` (your Backend URL)
- [ ] Frontend: `VITE_WS_URL` (your Backend URL)

---

## Troubleshooting

### "Cannot connect to backend"
- Check Backend service is running
- Verify `VITE_API_BASE_URL` is correct
- Check Backend logs: `railway logs --service backend`

### "Ollama connection failed"
- Check `LANGCHAIN4J_OLLAMA_BASE_URL` is correct (no `:8080`)
- Test Ollama: `curl https://your-ollama.railway.app/api/tags`
- Verify model is pulled: `railway shell --service ollama` → `ollama list`

### "No response from agent"
- Check Backend logs for errors
- Verify `ALPHA_VANTAGE_API_KEY` is set
- Check Database connection

### Frontend shows blank page
- Check browser console for errors
- Verify environment variables are set
- Check Frontend build logs

---

## Testing the Full Stack

### 1. Test Ollama
```bash
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

### 2. Test Backend
```bash
curl https://your-backend.railway.app/api/advisor/status
```

### 3. Test Frontend
- Open Frontend URL in browser
- Should see the chat interface

---

## Summary

**You can't chat directly with Ollama.** You need:

1. ✅ **Ollama** - Already deployed
2. ⏳ **Backend** - Deploy now, connect to Ollama
3. ⏳ **Frontend** - Deploy after backend, this is your chat UI

**The chat interface is the Frontend service** - that's where you'll actually chat with the agent!

Once all 3 services are deployed and connected, open the **Frontend URL** in your browser to start chatting.

---

## Next Steps

1. Deploy Backend service (Step 2)
2. Deploy Frontend service (Step 3)
3. Open Frontend URL in browser
4. Start chatting! 💬

For detailed deployment steps, see [RAILWAY_DEPLOYMENT.md](./RAILWAY_DEPLOYMENT.md)

