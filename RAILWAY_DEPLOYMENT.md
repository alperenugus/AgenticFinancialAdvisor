# Railway Deployment Guide - Step by Step

This guide will walk you through deploying the entire Agentic Financial Advisor system on Railway.

## Prerequisites

1. **GitHub Account** - Your code should be on GitHub
2. **Railway Account** - Sign up at [railway.app](https://railway.app) (free tier available)
3. **API Keys** (optional but recommended):
   - Alpha Vantage API key: [Get free key](https://www.alphavantage.co/support/#api-key)
   - NewsAPI key (optional): [Get free key](https://newsapi.org/)

---

## Step 1: Create Railway Project

1. Go to [railway.app](https://railway.app) and sign in
2. Click **"New Project"**
3. Select **"Deploy from GitHub repo"**
4. Choose your repository: `alperenugus/AgenticFinancialAdvisor`
5. Railway will create a new project

---

## Step 2: Deploy PostgreSQL Database

1. In your Railway project, click **"New"** → **"Database"** → **"PostgreSQL"**
2. Railway will automatically:
   - Create a PostgreSQL database
   - Set the `DATABASE_URL` environment variable
   - Provide connection details

**Note**: The `DATABASE_URL` will be automatically available to all services in your project.

---

## Step 3: Deploy Ollama Service (FREE LLM!)

### 3.1 Create Ollama Service

1. In Railway project, click **"New"** → **"Empty Service"**
2. Name it: `ollama` or `ollama-service`
3. Click on the service to open it

### 3.2 Configure Ollama Service

1. Go to **Settings** → **Source**
2. Set **Root Directory** to: `backend` (since Dockerfile.ollama is in backend folder)
3. Go to **Settings** → **Deploy**
4. Set **Dockerfile Path** to: `Dockerfile.ollama`
5. Railway will automatically detect and build

**Alternative**: You can also create the Dockerfile directly in Railway:
- Go to **Settings** → **Source** → **Add Dockerfile**
- Copy content from `backend/Dockerfile.ollama`

### 3.3 Deploy and Get URL

1. Railway will automatically build and deploy
2. Wait 2-3 minutes for deployment
3. Once deployed, go to **Settings** → **Networking**
4. Copy the **Public Domain** URL (e.g., `https://ollama-production-xxxx.up.railway.app`)
5. **Save this URL** - you'll need it for the backend!

### 3.4 Pull the Model

After Ollama is deployed, you need to pull the model:

```bash
# Install Railway CLI (if not installed)
npm i -g @railway/cli

# Login to Railway
railway login

# Link to your project (select your project when prompted)
railway link

# Connect to Ollama service
railway shell --service ollama

# Pull the model (this downloads ~4GB, takes 5-10 minutes)
ollama pull llama3.1

# Verify model is available
ollama list
```

You should see `llama3.1` in the list.

---

## Step 4: Deploy Backend Service

### 4.1 Create Backend Service

1. In Railway project, click **"New"** → **"GitHub Repo"**
2. Select your repository again
3. Railway will create a new service

### 4.2 Configure Backend Service

1. Click on the backend service
2. Go to **Settings** → **Source**
3. Set **Root Directory** to: `backend`
4. Go to **Settings** → **Deploy**
5. Railway will auto-detect the Dockerfile in `backend/Dockerfile`

### 4.3 Set Environment Variables

Go to **Variables** tab and add these environment variables:

#### Required Variables:

```bash
# Ollama Configuration (use the URL from Step 3.3)
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Database (Railway auto-sets this, but you can verify)
# DATABASE_URL is automatically set by Railway PostgreSQL service

# Market Data API (get free key from https://www.alphavantage.co/support/#api-key)
ALPHA_VANTAGE_API_KEY=your_alpha_vantage_api_key_here

# Server Port (Railway sets this automatically)
PORT=8080
```

#### Optional Variables:

```bash
# CORS Origins (if frontend is on different domain)
CORS_ORIGINS=https://your-frontend.railway.app

# News API (optional)
NEWS_API_KEY=your_news_api_key_here
```

**Important**: Replace `https://your-ollama-service.railway.app` with the actual Ollama URL from Step 3.3!

### 4.4 Deploy Backend

1. Railway will automatically build and deploy
2. Wait for deployment to complete (usually 3-5 minutes)
3. Once deployed, go to **Settings** → **Networking**
4. Copy the **Public Domain** URL (e.g., `https://backend-production-xxxx.up.railway.app`)
5. **Save this URL** - you'll need it for the frontend!

---

## Step 5: Deploy Frontend Service

### 5.1 Create Frontend Service

1. In Railway project, click **"New"** → **"GitHub Repo"**
2. Select your repository again
3. Railway will create a new service

### 5.2 Configure Frontend Service

1. Click on the frontend service
2. Go to **Settings** → **Source**
3. Set **Root Directory** to: `frontend`
4. Go to **Settings** → **Deploy**

### 5.3 Set Build Settings

In **Settings** → **Deploy**, configure:

- **Build Command**: `npm install && npm run build`
- **Start Command**: `npx serve -s dist -p $PORT`
- **Output Directory**: `dist`

### 5.4 Set Environment Variables

Go to **Variables** tab and add:

```bash
# Backend API URL (use the URL from Step 4.4)
VITE_API_BASE_URL=https://your-backend.railway.app/api

# WebSocket URL (use the URL from Step 4.4)
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (Railway sets this automatically)
PORT=3000
```

**Important**: Replace `https://your-backend.railway.app` with the actual backend URL from Step 4.4!

### 5.5 Deploy Frontend

1. Railway will automatically build and deploy
2. Wait for deployment to complete (usually 2-3 minutes)
3. Once deployed, go to **Settings** → **Networking**
4. Copy the **Public Domain** URL
5. Your frontend is now live! 🎉

---

## Step 6: Verify Deployment

### 6.1 Test Backend

```bash
# Test backend health
curl https://your-backend.railway.app/api/advisor/status

# Should return JSON with agent status
```

### 6.2 Test Ollama Connection

```bash
# Test Ollama is accessible
curl https://your-ollama-service.railway.app/api/tags

# Should return list of models (including llama3.1)
```

### 6.3 Test Frontend

1. Open your frontend URL in a browser
2. You should see the application
3. Try creating a user profile
4. Try asking a question in the chat

---

## Complete Environment Variables Summary

### Backend Service Variables:

```bash
# Ollama (REQUIRED)
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Database (AUTO-SET by Railway)
DATABASE_URL=postgresql://user:pass@host:5432/dbname

# Market Data (REQUIRED)
ALPHA_VANTAGE_API_KEY=your_key_here

# Server (AUTO-SET by Railway)
PORT=8080

# CORS (OPTIONAL - if frontend on different domain)
CORS_ORIGINS=https://your-frontend.railway.app

# News API (OPTIONAL)
NEWS_API_KEY=your_key_here
```

### Frontend Service Variables:

```bash
# Backend API (REQUIRED)
VITE_API_BASE_URL=https://your-backend.railway.app/api

# WebSocket (REQUIRED)
VITE_WS_URL=https://your-backend.railway.app/ws

# Port (AUTO-SET by Railway)
PORT=3000
```

---

## Troubleshooting

### Backend Can't Connect to Ollama

1. **Check Ollama URL**: Make sure `LANGCHAIN4J_OLLAMA_BASE_URL` is correct
2. **Check Ollama is Running**: Go to Ollama service → check status is "Active"
3. **Check Model**: SSH into Ollama service and run `ollama list` to verify model exists
4. **Test Connection**: 
   ```bash
   curl https://your-ollama-service.railway.app/api/tags
   ```

### Model Not Found Error

```bash
# SSH into Ollama service
railway shell --service ollama

# Pull the model
ollama pull llama3.1

# Verify
ollama list
```

### Database Connection Issues

1. **Check DATABASE_URL**: Should be auto-set by Railway PostgreSQL
2. **Verify Database Service**: Make sure PostgreSQL service is running
3. **Check Logs**: 
   ```bash
   railway logs --service backend
   ```

### Frontend Can't Connect to Backend

1. **Check API URL**: Verify `VITE_API_BASE_URL` is correct
2. **Check CORS**: Make sure backend has `CORS_ORIGINS` set to frontend URL
3. **Check Backend Status**: Verify backend service is running
4. **Check Browser Console**: Look for CORS or connection errors

### Build Failures

1. **Check Logs**: 
   ```bash
   railway logs --service backend
   railway logs --service frontend
   ```
2. **Check Dependencies**: Make sure all dependencies are in `package.json` or `pom.xml`
3. **Check Dockerfile**: Verify Dockerfile paths are correct

---

## Cost Estimate

### Railway Free Tier:
- **$5 credit/month** (free tier)
- **500 hours** of usage
- **5GB** bandwidth

### Estimated Monthly Cost:
- **Ollama Service**: ~$2-3/month (depending on usage)
- **Backend Service**: ~$2-3/month
- **Frontend Service**: ~$1/month (static hosting)
- **PostgreSQL**: ~$1-2/month (small database)
- **Total**: ~$6-9/month (may exceed free tier for heavy usage)

**Note**: For light usage, you may stay within the free tier!

---

## Next Steps

1. ✅ All services deployed
2. ✅ Environment variables set
3. ✅ Models pulled
4. ✅ Test the application
5. 🎉 Share your deployed app!

---

## Quick Reference

### Railway CLI Commands:

```bash
# Login
railway login

# Link to project
railway link

# View logs
railway logs

# View logs for specific service
railway logs --service backend

# SSH into service
railway shell --service ollama

# Set environment variable
railway variables set KEY=value

# View variables
railway variables
```

### Service URLs:

- **Ollama**: `https://your-ollama-service.railway.app`
- **Backend**: `https://your-backend.railway.app`
- **Frontend**: `https://your-frontend.railway.app`
- **Database**: Managed by Railway (internal connection)

---

**Need Help?** Check the main [README.md](./README.md) or [DEPLOYMENT.md](./DEPLOYMENT.md) for more details.

