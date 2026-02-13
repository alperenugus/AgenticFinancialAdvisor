# Fix: Ollama Not Accessible from Outside

## Problem

You have a public Railway URL but can't access Ollama from outside. This is usually because:

1. **Ollama not listening on all interfaces** (0.0.0.0)
2. **Port not properly exposed**
3. **Service not running**
4. **Railway networking configuration**

---

## Quick Fix

### 1. Verify Ollama is Running

In Railway dashboard:
- Go to Ollama service
- Check status: Should be **"Running"** (green)
- If not, check logs for errors

### 2. Test the URL

```bash
# Test if Ollama API is accessible
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags

# Should return JSON with list of models
```

**If this fails**, continue with fixes below.

---

## Common Issues & Fixes

### Issue 1: Ollama Not Listening on 0.0.0.0

**Check:** Your Dockerfile should have:
```dockerfile
ENV OLLAMA_HOST=0.0.0.0:11434
```

**Fix:** Verify `ollama/Dockerfile` has this line. If not, add it.

### Issue 2: Port Not Exposed

**Check:** Dockerfile should expose port 11434:
```dockerfile
EXPOSE 11434
```

**Fix:** Make sure this is in your Dockerfile.

### Issue 3: Railway Service Not Public

**Check in Railway:**
1. Go to Ollama service
2. **Settings** → **Networking**
3. Make sure **"Public Domain"** is enabled
4. Copy the public URL (should match yours)

**Fix:** If no public domain, Railway might be using internal networking only.

### Issue 4: Service Not Running

**Check logs:**
```bash
railway logs --service ollama
```

**Look for:**
- ✅ "Ollama service is running" = Good
- ❌ "Error" or "Failed" = Service crashed
- ❌ "Connection refused" = Not listening

---

## Verify Configuration

### Check Dockerfile

Your `ollama/Dockerfile` should have:

```dockerfile
FROM ollama/ollama:latest

# Install curl
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 11434

# CRITICAL: Listen on all interfaces
ENV OLLAMA_HOST=0.0.0.0:11434
ENV OLLAMA_KEEP_ALIVE=24h

# Copy and use entrypoint
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
```

**Key line:** `ENV OLLAMA_HOST=0.0.0.0:11434`

This makes Ollama listen on all network interfaces, not just localhost.

---

## Testing Access

### From Your Computer

```bash
# Test API endpoint
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags

# Test generate endpoint
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/generate \
  -d '{"model": "llama3.1", "prompt": "Hello"}'
```

### Expected Responses

**Success (api/tags):**
```json
{
  "models": [
    {
      "name": "llama3.1",
      "modified_at": "...",
      "size": 1234567890
    }
  ]
}
```

**Success (api/generate):**
```json
{
  "model": "llama3.1",
  "response": "Hello! How can I help you?",
  ...
}
```

**Failure:**
- Connection refused
- Timeout
- 404 Not Found

---

## Railway-Specific Issues

### Issue: Railway Internal Networking

Railway services can be:
- **Public** - Accessible from internet (what you want)
- **Private** - Only accessible within Railway network

**Check:**
1. Ollama service → **Settings** → **Networking**
2. Look for **"Public Domain"** section
3. Should show your URL: `agenticfinancialadvisorollama-production.up.railway.app`
4. If missing, Railway might not have assigned a public domain

**Fix:**
- Railway should auto-assign public domains
- If not showing, check service is deployed and running
- Try redeploying the service

### Issue: Port Configuration

Railway automatically handles port mapping. You don't need to specify `:11434` in the URL.

**Correct URL:**
```
https://agenticfinancialadvisorollama-production.up.railway.app
```

**Wrong:**
```
https://agenticfinancialadvisorollama-production.up.railway.app:11434
```

Railway routes traffic automatically.

---

## Debugging Steps

### Step 1: Check Service Status

```bash
railway status
# or in dashboard, check Ollama service is "Running"
```

### Step 2: Check Logs

```bash
railway logs --service ollama --follow
```

**Look for:**
- "Ollama service is running"
- "Model llama3.1 already exists"
- Any error messages

### Step 3: Test from Inside Railway

```bash
# SSH into Ollama service
railway shell --service ollama

# Test locally
curl http://localhost:11434/api/tags

# If this works but external doesn't, it's a networking issue
```

### Step 4: Test External Access

```bash
# From your computer
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

### Step 5: Check Railway Networking

1. Ollama service → **Settings** → **Networking**
2. Verify public domain is assigned
3. Check if there are any restrictions

---

## Common Error Messages

### "Connection Refused"

**Cause:** Ollama not listening on 0.0.0.0

**Fix:** Add `ENV OLLAMA_HOST=0.0.0.0:11434` to Dockerfile

### "Timeout"

**Cause:** Service not running or not accessible

**Fix:** 
- Check service is running
- Check Railway networking settings
- Verify public domain is enabled

### "404 Not Found"

**Cause:** Wrong URL or path

**Fix:** 
- Use: `https://agenticfinancialadvisorollama-production.up.railway.app/api/tags`
- Don't add port number
- Don't add `/ollama` or other paths

### "Service Unavailable"

**Cause:** Service crashed or not started

**Fix:**
- Check logs: `railway logs --service ollama`
- Redeploy service
- Check if model was pulled successfully

---

## Quick Checklist

- [ ] Ollama service status: **Running** (green in Railway)
- [ ] Dockerfile has: `ENV OLLAMA_HOST=0.0.0.0:11434`
- [ ] Dockerfile has: `EXPOSE 11434`
- [ ] Railway public domain is assigned
- [ ] Service logs show "Ollama service is running"
- [ ] Model is pulled: `ollama list` shows `llama3.1`
- [ ] Test from inside Railway works: `curl http://localhost:11434/api/tags`
- [ ] Test from outside works: `curl https://...railway.app/api/tags`

---

## If Still Not Working

### Option 1: Redeploy Service

1. Go to Ollama service in Railway
2. **Deployments** → **Redeploy**
3. Wait for deployment
4. Test again

### Option 2: Check Railway Plan

Some Railway plans might have restrictions on public domains. Check your plan limits.

### Option 3: Use Railway Internal URL

If public domain doesn't work, you can use Railway's internal service discovery:

In your backend, instead of:
```
LANGCHAIN4J_OLLAMA_BASE_URL=https://agenticfinancialadvisorollama-production.up.railway.app
```

Use Railway's internal service name (if available):
```
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama:11434
```

But this only works for services within the same Railway project.

---

## Summary

**Most likely issue:** Ollama not listening on `0.0.0.0:11434`

**Quick fix:** Verify Dockerfile has:
```dockerfile
ENV OLLAMA_HOST=0.0.0.0:11434
```

**Test:**
```bash
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

If this returns JSON with models, Ollama is accessible! ✅

If not, check the service logs and Railway networking settings.

