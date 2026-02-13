# Fix: Railway Nixpacks "Could Not Determine How to Build"

## Problem

Railway is trying to auto-detect your build process (Nixpacks) but can't determine how to build. This happens when Railway doesn't find clear build instructions.

## Solution: Force Dockerfile Build

Railway needs explicit configuration to use Dockerfiles instead of Nixpacks.

---

## Fix for Backend Service

### Option 1: Use Railway Dashboard (Recommended)

1. Go to your **Backend** service in Railway
2. Go to **Settings** → **Deploy**
3. Under **Build Settings**, set:
   - **Builder**: `Dockerfile`
   - **Dockerfile Path**: `Dockerfile`
   - **Docker Build Context**: `backend` (or leave empty if root is backend)
4. **Save** and redeploy

### Option 2: Use railway.toml (Already Created)

The `backend/railway.toml` file is already configured. Railway should detect it automatically.

If not working:
1. Make sure Root Directory is set to `backend` in Railway Settings → Source
2. Redeploy the service

### Option 3: Use railway.json (Alternative)

The `backend/railway-backend.json` can be renamed to `railway.json` if needed.

---

## Fix for Ollama Service

### Railway Dashboard Settings

1. Go to your **Ollama** service in Railway
2. Go to **Settings** → **Deploy**
3. Under **Build Settings**, set:
   - **Builder**: `Dockerfile`
   - **Dockerfile Path**: `Dockerfile.ollama`
   - **Root Directory**: `backend` (since Dockerfile.ollama is in backend folder)
4. **Save** and redeploy

### railway.json (Already Configured)

The `backend/railway.json` is already configured for Ollama. Make sure:
- Root Directory is set to `backend` in Railway
- Railway detects the `railway.json` file

---

## Step-by-Step Fix

### 1. Backend Service

**In Railway Dashboard:**

1. Click on your **Backend** service
2. Go to **Settings** → **Source**
   - **Root Directory**: `backend`
   - **Branch**: `main` (or your branch)
3. Go to **Settings** → **Deploy**
   - **Builder**: Select `Dockerfile`
   - **Dockerfile Path**: `Dockerfile`
   - **Docker Build Context**: Leave empty (or set to `.`)
4. Click **Save**
5. Go to **Deployments** tab
6. Click **Redeploy** or trigger a new deployment

**Verify:**
- Check build logs - should show "Building Dockerfile"
- Should NOT show "Nixpacks detected" or "Could not determine"

### 2. Ollama Service

**In Railway Dashboard:**

1. Click on your **Ollama** service
2. Go to **Settings** → **Source**
   - **Root Directory**: `backend`
   - **Branch**: `main`
3. Go to **Settings** → **Deploy**
   - **Builder**: Select `Dockerfile`
   - **Dockerfile Path**: `Dockerfile.ollama`
   - **Docker Build Context**: Leave empty
4. Click **Save**
5. Redeploy

---

## Alternative: Use Railway CLI

If dashboard doesn't work, use CLI:

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# Set build configuration for backend
railway variables set RAILWAY_BUILDER=DOCKERFILE --service backend
railway variables set RAILWAY_DOCKERFILE_PATH=Dockerfile --service backend

# Set build configuration for Ollama
railway variables set RAILWAY_BUILDER=DOCKERFILE --service ollama
railway variables set RAILWAY_DOCKERFILE_PATH=Dockerfile.ollama --service ollama

# Redeploy
railway up --service backend
railway up --service ollama
```

---

## Verify Build is Working

### Check Build Logs

```bash
# View backend build logs
railway logs --service backend

# Look for:
# ✅ "Building Dockerfile"
# ✅ "Step 1/10 : FROM maven:3.9-eclipse-temurin-21"
# ❌ NOT "Nixpacks detected"
# ❌ NOT "Could not determine how to build"
```

### Expected Backend Build Output

```
Building Dockerfile
Step 1/10 : FROM maven:3.9-eclipse-temurin-21 AS build
Step 2/10 : WORKDIR /app
Step 3/10 : COPY pom.xml .
...
Successfully built <image-id>
```

### Expected Ollama Build Output

```
Building Dockerfile.ollama
Step 1/5 : FROM ollama/ollama:latest
...
Successfully built <image-id>
```

---

## If Still Not Working

### 1. Check File Structure

Make sure files exist:
```
backend/
├── Dockerfile          ✅
├── Dockerfile.ollama  ✅
├── railway.toml       ✅
├── railway.json       ✅
└── pom.xml            ✅
```

### 2. Check Root Directory

In Railway Settings → Source:
- Backend: Root Directory = `backend`
- Ollama: Root Directory = `backend`

### 3. Manual Dockerfile Path

In Railway Settings → Deploy:
- Backend: Dockerfile Path = `Dockerfile` (not `backend/Dockerfile`)
- Ollama: Dockerfile Path = `Dockerfile.ollama` (not `backend/Dockerfile.ollama`)

### 4. Force Rebuild

```bash
# Delete and recreate service (last resort)
# Or use Railway dashboard → Settings → Delete Service
# Then create new service with correct settings
```

---

## Quick Checklist

### Backend Service
- [ ] Root Directory: `backend`
- [ ] Builder: `Dockerfile`
- [ ] Dockerfile Path: `Dockerfile`
- [ ] Build logs show "Building Dockerfile"
- [ ] No Nixpacks errors

### Ollama Service
- [ ] Root Directory: `backend`
- [ ] Builder: `Dockerfile`
- [ ] Dockerfile Path: `Dockerfile.ollama`
- [ ] Build logs show "Building Dockerfile.ollama"
- [ ] No Nixpacks errors

---

## Common Mistakes

1. **Wrong Root Directory**
   - ❌ Root Directory: `/` (root of repo)
   - ✅ Root Directory: `backend`

2. **Wrong Dockerfile Path**
   - ❌ Dockerfile Path: `backend/Dockerfile`
   - ✅ Dockerfile Path: `Dockerfile` (relative to root directory)

3. **Builder Not Set**
   - ❌ Builder: Auto-detect (Nixpacks)
   - ✅ Builder: Dockerfile

4. **Missing Files**
   - Make sure `Dockerfile` exists in `backend/` folder
   - Make sure `Dockerfile.ollama` exists in `backend/` folder

---

## Still Having Issues?

1. **Check Railway Logs:**
   ```bash
   railway logs --service backend
   ```

2. **Check Build Output:**
   - Go to Railway Dashboard → Deployments
   - Click on latest deployment
   - Check build logs for errors

3. **Try Manual Build Locally:**
   ```bash
   cd backend
   docker build -t test-backend .
   # If this works, Railway should work too
   ```

4. **Contact Railway Support:**
   - They can help debug build issues
   - Provide them with build logs

---

**Remember**: The key is setting **Builder = Dockerfile** in Railway Settings → Deploy, not relying on auto-detection!

