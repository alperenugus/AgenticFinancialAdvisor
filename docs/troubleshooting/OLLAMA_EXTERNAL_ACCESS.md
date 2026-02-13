# Fix: Ollama External API Not Accessible

## Problem

You can't access Ollama's external API:
```bash
curl https://your-ollama.railway.app/api/tags
# Returns: 502 Bad Gateway or Connection Refused
```

But Ollama is running internally (logs show it's working).

## Root Cause

Railway routes external traffic to the **`$PORT` environment variable**. If:
1. `$PORT` is not set in Railway
2. Ollama is listening on a different port than `$PORT`
3. Railway can't route traffic correctly

Then external access will fail.

---

## Solution: Set PORT Environment Variable

### Step 1: Set PORT in Railway

1. Go to **Ollama service** in Railway Dashboard
2. Click **Settings** → **Variables**
3. Add new variable:
   - **Name**: `PORT`
   - **Value**: `11434`
4. Click **Save**
5. **Redeploy** the service

### Step 2: Verify Configuration

After redeploying, check logs:
```bash
railway logs --service ollama
```

Look for:
```
Ollama Configuration:
  PORT env var: 11434
  OLLAMA_PORT: 11434
  OLLAMA_HOST: 0.0.0.0:11434
```

### Step 3: Test External Access

```bash
# Test from your computer
curl https://your-ollama.railway.app/api/tags

# Should return JSON with models list
```

---

## Why This Happens

### Railway Port Routing

Railway's external routing works like this:

1. **External Request** → `https://service.railway.app/api/tags`
2. **Railway Router** → Routes to `$PORT` on the service
3. **Service** → Must be listening on `$PORT`

**If `$PORT` is not set:**
- Railway doesn't know which port to route to
- External requests fail with 502

**If service listens on wrong port:**
- Railway routes to `$PORT` but service is on different port
- Connection refused

---

## Alternative: Use Railway's Auto-Port

Railway sometimes auto-sets `$PORT`. Check:

1. Ollama service → **Settings** → **Variables**
2. Look for `PORT` variable
3. If it exists, note the value
4. Make sure Ollama listens on that port

**If Railway sets PORT to something other than 11434:**

You have two options:

### Option A: Change Ollama to use Railway's PORT
- Ollama will automatically use `$PORT` from entrypoint.sh
- No changes needed if PORT is set

### Option B: Force PORT to 11434
- Set `PORT=11434` in Railway variables
- Ollama will use 11434
- Railway will route to 11434

---

## Verification Checklist

- [ ] `PORT` environment variable is set in Railway
- [ ] Ollama service is running (green status)
- [ ] Logs show: `OLLAMA_HOST: 0.0.0.0:11434` (or whatever PORT is set)
- [ ] Public domain is enabled in Railway (Settings → Networking)
- [ ] Health check is passing
- [ ] External curl test works

---

## Debugging Steps

### Step 1: Check PORT Variable

```bash
# Via Railway CLI
railway variables --service ollama | grep PORT

# Should show:
# PORT=11434
```

### Step 2: Check Service Logs

```bash
railway logs --service ollama | grep -i "ollama.*listen\|PORT\|OLLAMA_HOST"
```

Look for:
- `OLLAMA_HOST: 0.0.0.0:11434` ✅
- `PORT env var: 11434` ✅

### Step 3: Test Internal Access

```bash
# SSH into Railway service
railway shell --service ollama

# Test from inside
curl http://localhost:11434/api/tags
```

If this works but external doesn't → Port routing issue

### Step 4: Check Railway Networking

1. Ollama service → **Settings** → **Networking**
2. Verify **Public Domain** is set
3. Copy the URL
4. Test: `curl https://<public-domain>/api/tags`

---

## Common Issues

### Issue: PORT Not Set

**Symptom:** Logs show `PORT env var: not set`

**Fix:** Set `PORT=11434` in Railway variables

### Issue: Wrong Port

**Symptom:** Ollama listening on 11434 but PORT=8080

**Fix:** Either:
- Set `PORT=11434` in Railway
- Or let Railway set PORT and Ollama will auto-use it

### Issue: Service Not Public

**Symptom:** No public domain in Settings → Networking

**Fix:** 
- Railway should auto-assign public domains
- If missing, redeploy service
- Check service is not in "Private" mode

---

## Quick Fix Summary

```bash
# 1. Set PORT in Railway Dashboard
PORT=11434

# 2. Redeploy service
# (Railway will auto-redeploy on variable change, or manually redeploy)

# 3. Test
curl https://your-ollama.railway.app/api/tags
```

---

## Still Not Working?

1. **Check Railway Status**: Service must be "Running" (green)
2. **Check Health Check**: Should be passing
3. **Check Logs**: Look for errors or warnings
4. **Try Internal URL**: Use `http://ollama.railway.internal:11434` for backend (works even if external doesn't)
5. **Contact Railway Support**: If service is running but external access fails

---

## Why Use Internal URL Instead?

If external access keeps failing, **use internal URL for backend**:

```bash
# In Backend service variables:
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama.railway.internal:11434
```

**Benefits:**
- ✅ Works even if external routing is broken
- ✅ Faster (no external routing)
- ✅ More reliable
- ✅ No PORT configuration needed

**See:** [OLLAMA_INTERNAL_URL.md](../railway/OLLAMA_INTERNAL_URL.md)

