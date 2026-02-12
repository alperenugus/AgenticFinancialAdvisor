# Railway UI Guide - Current Interface

Railway's UI has changed over time. Here's how to configure Dockerfiles in the **current Railway interface**.

---

## Method 1: Using Root Directory (Recommended)

Railway automatically detects Dockerfiles based on the **Root Directory** setting.

### For Backend Service:

1. Go to your **Backend** service
2. Click **Settings** → **Source**
3. Set **Root Directory**: `backend`
4. Railway will automatically look for `Dockerfile` in that directory
5. **No need to specify Dockerfile path** - Railway finds it automatically

### For Ollama Service:

1. Go to your **Ollama** service  
2. Click **Settings** → **Source**
3. Set **Root Directory**: `backend`
4. Railway will look for Dockerfile, but we need to tell it to use `Dockerfile.ollama`

**Problem**: Railway might not auto-detect `Dockerfile.ollama`

**Solution**: Use one of these methods:

---

## Method 2: Rename Dockerfile (Easiest for Ollama)

Since Railway auto-detects `Dockerfile`, you can:

1. **For Ollama service**: 
   - Temporarily rename `Dockerfile.ollama` to `Dockerfile` in your repo
   - OR create a separate branch/folder
   - OR use Method 3 below

**Better Solution**: Use `railway.toml` or `railway.json` (Method 3)

---

## Method 3: Use railway.toml or railway.json (Best)

Railway reads configuration files from your repo. These files tell Railway which Dockerfile to use.

### For Backend Service:

The `backend/railway.toml` file is already configured:

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"
```

**Steps:**
1. Go to **Backend** service → **Settings** → **Source**
2. Set **Root Directory**: `backend`
3. Railway will read `backend/railway.toml` automatically
4. It will use `Dockerfile` as specified

### For Ollama Service:

The `backend/railway.json` is already configured for Ollama:

```json
{
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile.ollama"
  }
}
```

**Steps:**
1. Go to **Ollama** service → **Settings** → **Source**
2. Set **Root Directory**: `backend`
3. Railway will read `backend/railway.json` automatically
4. It will use `Dockerfile.ollama` as specified

**Note**: Railway might prioritize `railway.toml` over `railway.json`. If both exist, you might need to:

**Option A**: Rename `railway.json` to `railway-ollama.json` and create a separate one, OR

**Option B**: Use different root directories (see Method 4)

---

## Method 4: Separate Root Directories (Alternative)

If Railway can't distinguish between services:

### Backend Service:
- **Root Directory**: `backend`
- Railway reads `backend/railway.toml` → uses `Dockerfile`

### Ollama Service:
- **Root Directory**: `backend` (same)
- Railway reads `backend/railway.json` → uses `Dockerfile.ollama`

**If this doesn't work**, create a separate folder:

1. Create `backend-ollama/` folder
2. Copy `Dockerfile.ollama` to `backend-ollama/Dockerfile`
3. Set Ollama service Root Directory: `backend-ollama`

---

## Method 5: Railway CLI (If UI Doesn't Work)

If the UI doesn't have the options, use Railway CLI:

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# Set build configuration via CLI
railway variables set RAILWAY_BUILDER=DOCKERFILE --service backend
railway variables set RAILWAY_DOCKERFILE_PATH=Dockerfile --service backend

railway variables set RAILWAY_BUILDER=DOCKERFILE --service ollama  
railway variables set RAILWAY_DOCKERFILE_PATH=Dockerfile.ollama --service ollama
```

---

## Current Railway UI Locations

### Where to Find Settings:

1. **Service Settings**:
   - Click on your service name
   - Click **"Settings"** tab (top right)
   - Or click the gear icon ⚙️

2. **Source Settings**:
   - Settings → **"Source"** section
   - Look for:
     - **Root Directory** (this is what you need!)
     - **Branch** (usually `main`)
     - **Repository** (your GitHub repo

3. **Deploy Settings** (if available):
   - Settings → **"Deploy"** section
   - Look for:
     - **Build Command** (leave empty for Dockerfile)
     - **Start Command** (leave empty for Dockerfile)
     - **Builder** (might show "Dockerfile" or "Nixpacks")

---

## Step-by-Step: What You Should See

### Backend Service Configuration:

```
Service: backend
├── Settings → Source
│   ├── Repository: alperenugus/AgenticFinancialAdvisor
│   ├── Branch: main
│   └── Root Directory: backend  ← SET THIS!
│
└── Settings → Deploy (if visible)
    ├── Builder: Dockerfile (or Auto-detect)
    └── Build Command: (empty)
```

### Ollama Service Configuration:

```
Service: ollama
├── Settings → Source
│   ├── Repository: alperenugus/AgenticFinancialAdvisor
│   ├── Branch: main
│   └── Root Directory: backend  ← SET THIS!
│
└── Settings → Deploy (if visible)
    ├── Builder: Dockerfile (or Auto-detect)
    └── Build Command: (empty)
```

---

## If Railway Still Uses Nixpacks

If Railway still tries to use Nixpacks after setting Root Directory:

1. **Check if railway.toml/railway.json exists** in the root directory
2. **Verify Root Directory** is set correctly
3. **Try creating a `.railwayignore` file** to force Dockerfile:

Create `backend/.railwayignore`:
```
# Force Railway to use Dockerfile
```

4. **Or add a `railway.toml` at repo root**:

Create `railway.toml` at project root:
```toml
[build]
builder = "DOCKERFILE"
```

---

## Quick Fix: Copy Dockerfile.ollama to Separate Location

If nothing works, create a separate structure:

```bash
# In your repo, create:
backend-ollama/
└── Dockerfile  (copy from backend/Dockerfile.ollama)
```

Then:
- Ollama service Root Directory: `backend-ollama`
- Backend service Root Directory: `backend`

---

## Verify It's Working

After configuration, check build logs:

1. Go to service → **Deployments** tab
2. Click on latest deployment
3. Check build logs

**Should see:**
```
✅ Building Dockerfile
✅ Step 1/10 : FROM maven:3.9-eclipse-temurin-21
```

**Should NOT see:**
```
❌ Nixpacks detected
❌ Could not determine how to build
```

---

## Summary

**Most Important Setting:**
- **Root Directory**: Set this to `backend` for both services
- Railway will read `railway.toml` or `railway.json` from that directory
- These files specify which Dockerfile to use

**If Root Directory doesn't work:**
- Use Railway CLI to set variables
- Or create separate directories
- Or rename Dockerfile.ollama to Dockerfile temporarily

---

## Still Having Issues?

1. **Screenshot your Railway Settings page** and share what options you see
2. **Check Railway documentation**: https://docs.railway.app
3. **Try Railway CLI** method (Method 5 above)
4. **Contact Railway support** - they're very responsive

The key is: **Root Directory** + **railway.toml/railway.json** files should work together to tell Railway which Dockerfile to use.

