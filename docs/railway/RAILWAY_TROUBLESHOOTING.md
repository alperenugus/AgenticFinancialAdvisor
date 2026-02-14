# Railway Troubleshooting Guide

## "The train has not arrived at the station" Error

This error means Railway cannot find or access your service. Here's how to fix it:

---

## Step 1: Check Service Status

1. **Go to Railway Dashboard**
   - Visit [railway.app](https://railway.app)
   - Open your project
   - Check each service's status

2. **Check Service Status Indicators**
   - 🟢 **Green** = Running
   - 🟡 **Yellow** = Building/Starting
   - 🔴 **Red** = Failed/Stopped
   - ⚪ **Gray** = Not deployed

---

## Step 2: Check Build Logs

1. **Click on the service** that's showing the error
2. **Go to "Deployments" tab**
3. **Click on the latest deployment**
4. **Check the build logs** for errors:

### Common Build Errors:

#### Error: "Dockerfile not found"
**Solution:**
- Go to **Settings → Source**
- Set **Root Directory** correctly (e.g., `backend` for backend service)
- Set **Dockerfile Path** correctly (e.g., `Dockerfile`)

#### Error: "Build failed" or "Maven build failed"
**Solution:**
- Check if all dependencies are available
- Verify `pom.xml` is correct
- Check if Java 21 is specified correctly

#### Error: "Port not exposed"
**Solution:**
- Ensure Dockerfile has `EXPOSE 8080` (or your port)
- Check `railway.toml` has correct health check path

---

## Step 3: Check Service Configuration

### Backend Service Configuration

1. **Settings → Source:**
   - ✅ Root Directory: `backend`
   - ✅ Dockerfile Path: `Dockerfile`

2. **Settings → Deploy:**
   - ✅ Builder: `Dockerfile`
   - ✅ Health Check Path: `/api/advisor/status`
   - ✅ Health Check Timeout: `300` (5 minutes)

3. **Settings → Variables:**
   - ✅ `DATABASE_URL` (auto-set by PostgreSQL service)
   - ✅ `GROQ_API_KEY` (required)
   - ✅ `GOOGLE_CLIENT_ID` (required for auth)
   - ✅ `GOOGLE_CLIENT_SECRET` (required for auth)
   - ✅ `JWT_SECRET` (required)
   - ✅ `CORS_ORIGINS` (set to your frontend URL)

### Frontend Service Configuration

1. **Settings → Source:**
   - ✅ Root Directory: `frontend`

2. **Settings → Deploy:**
   - ✅ Build Command: `npm install && npm run build`
   - ✅ Start Command: `npx serve -s dist -p $PORT`
   - ✅ Output Directory: `dist`

3. **Settings → Variables:**
   - ✅ `VITE_API_BASE_URL` (set to your backend URL)
   - ✅ `VITE_WS_URL` (set to your backend WebSocket URL)

---

## Step 4: Check Runtime Logs

1. **Click on the service**
2. **Go to "Logs" tab**
3. **Look for errors:**

### Common Runtime Errors:

#### Error: "Failed to connect to database"
**Solution:**
- Verify `DATABASE_URL` is set correctly
- Check PostgreSQL service is running
- Ensure database service is in the same Railway project

#### Error: "Connection refused" or "Cannot connect to Ollama"
**Solution:**
- **Note:** You're using Groq API now, not Ollama
- Verify `GROQ_API_KEY` is set correctly
- Check API key is valid

#### Error: "Port already in use"
**Solution:**
- Railway sets `PORT` automatically
- Don't hardcode port in your code
- Use `PORT` environment variable: `server.port=${PORT:8080}`

#### Error: "Application failed to respond"
**Solution:**
- Check health check endpoint is correct: `/api/advisor/status`
- Verify Spring Boot is actually starting
- Check logs for startup errors

---

## Step 5: Verify Service is Actually Running

### Check Health Endpoint

1. **Get your service URL:**
   - Go to **Settings → Networking**
   - Copy the **Public URL** (e.g., `https://your-service.railway.app`)

2. **Test health endpoint:**
   ```bash
   curl https://your-service.railway.app/api/advisor/status
   ```

3. **Expected response:**
   ```json
   {
     "status": "ok",
     "agents": {...}
   }
   ```

### If Health Check Fails:

1. **Check Spring Boot logs** for startup errors
2. **Verify all environment variables** are set
3. **Check database connection** is working
4. **Verify API keys** are valid

---

## Step 6: Common Fixes

### Fix 1: Service Not Deployed

**Problem:** Service shows as "Not Deployed"

**Solution:**
1. Go to **Settings → Source**
2. Ensure **Root Directory** is set correctly
3. Click **"Redeploy"** or **"Deploy"**

### Fix 2: Build Fails

**Problem:** Build logs show errors

**Solution:**
1. Check **Settings → Source** configuration
2. Verify Dockerfile exists in the root directory
3. Check for syntax errors in Dockerfile
4. Review build logs for specific errors

### Fix 3: Service Crashes on Startup

**Problem:** Service starts but immediately crashes

**Solution:**
1. Check **Logs** tab for error messages
2. Verify all required environment variables are set
3. Check database connection
4. Verify API keys are correct

### Fix 4: Health Check Fails

**Problem:** Service runs but health check fails

**Solution:**
1. Verify health check path: `/api/advisor/status`
2. Check if endpoint exists in your code
3. Increase health check timeout (try 300 seconds)
4. Verify service is listening on correct port

### Fix 5: Domain Not Provisioned

**Problem:** "Please check your network settings to confirm that your domain has provisioned"

**Solution:**
1. Go to **Settings → Networking**
2. Click **"Generate Domain"** if no domain exists
3. Wait 1-2 minutes for domain to provision
4. Try accessing the service again

---

## Step 7: Manual Redeploy

If nothing works, try a manual redeploy:

1. **Go to service**
2. **Click "Deployments" tab**
3. **Click "Redeploy"** on the latest deployment
4. **Or trigger a new deployment:**
   - Make a small change to your code
   - Commit and push to GitHub
   - Railway will auto-deploy

---

## Step 8: Check Service Dependencies

Ensure services are in the correct order:

1. **PostgreSQL** - Should be running first
2. **Backend** - Depends on PostgreSQL
3. **Frontend** - Depends on Backend

If Backend fails, Frontend will also fail.

---

## Step 9: Verify Environment Variables

### Backend Required Variables:

```bash
# Database (auto-set by Railway)
DATABASE_URL=postgresql://...

# Groq API (REQUIRED)
GROQ_API_KEY=your_groq_api_key

# Google OAuth (REQUIRED)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT (REQUIRED)
JWT_SECRET=your-256-bit-secret-key-minimum-32-characters

# CORS (REQUIRED - set to your frontend URL)
CORS_ORIGINS=https://your-frontend.railway.app

# Optional
ALPHA_VANTAGE_API_KEY=your_key
NEWS_API_KEY=your_key
```

### Frontend Required Variables:

```bash
# Backend API URL (REQUIRED)
VITE_API_BASE_URL=https://your-backend.railway.app/api

# WebSocket URL (REQUIRED)
VITE_WS_URL=wss://your-backend.railway.app/ws
```

---

## Step 10: Still Not Working?

### Get Help:

1. **Check Railway Status:** [status.railway.app](https://status.railway.app)
2. **Railway Discord:** [discord.gg/railway](https://discord.gg/railway)
3. **Railway Docs:** [docs.railway.app](https://docs.railway.app)

### Debug Checklist:

- [ ] Service is deployed (not "Not Deployed")
- [ ] Build completed successfully (check build logs)
- [ ] Service is running (check status indicator)
- [ ] All environment variables are set
- [ ] Health check endpoint is accessible
- [ ] Database service is running
- [ ] Domain is provisioned
- [ ] No errors in runtime logs
- [ ] Port is correctly configured
- [ ] CORS origins are set correctly

---

## Quick Fix Commands

### Check Service Status (Railway CLI):

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# Check service status
railway status

# View logs
railway logs

# Redeploy
railway up
```

---

## Example: Complete Service Check

1. **Backend Service:**
   - ✅ Status: Running (green)
   - ✅ Build: Success
   - ✅ Health Check: `/api/advisor/status` returns 200
   - ✅ Logs: No errors
   - ✅ Variables: All set

2. **Frontend Service:**
   - ✅ Status: Running (green)
   - ✅ Build: Success
   - ✅ Variables: `VITE_API_BASE_URL` set correctly
   - ✅ Logs: No errors

3. **Database Service:**
   - ✅ Status: Running (green)
   - ✅ Connection: Working

If all three are ✅, your app should be working!

