# Fix: Ollama Connection Refused (502 Error)

## Problem

You're seeing 502 errors with "connection refused" when accessing Ollama:
```
upstreamErrors: [{"error":"connection refused"}]
```

This means Railway can't connect to Ollama on the expected port.

## Root Cause

Railway routes external traffic to the `$PORT` environment variable, but:
1. `$PORT` might not be set in Railway
2. Ollama might not be listening on `$PORT`
3. Ollama might not have started properly

## Solution

### Step 1: Set PORT in Railway

1. Go to **Ollama service** in Railway Dashboard
2. **Settings** → **Variables**
3. Add/Update:
   ```bash
   PORT=11434
   ```
4. **Save** and **Redeploy**

### Step 2: Verify Ollama is Listening

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
Ollama is ready!
```

### Step 3: Test Internal Connection

```bash
# SSH into Railway
railway shell --service ollama

# Test from inside
curl http://localhost:11434/api/tags
```

If this works but external doesn't → Port routing issue

---

## Alternative: Use Internal URL for Backend

If external access keeps failing, use internal URL:

```bash
# In Backend service variables:
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama.railway.internal:11434
```

This bypasses Railway's external routing entirely.

---

## Debugging

### Check PORT Variable

```bash
railway variables --service ollama | grep PORT
```

Should show: `PORT=11434`

### Check Ollama Process

```bash
railway shell --service ollama
ps aux | grep ollama
```

Should show `ollama serve` process running.

### Check Listening Port

```bash
railway shell --service ollama
netstat -tlnp | grep 11434
# or
ss -tlnp | grep 11434
```

Should show Ollama listening on `0.0.0.0:11434` or `*:11434`.

---

## Common Issues

### Issue: PORT Not Set

**Symptom:** Logs show `PORT env var: not set`

**Fix:** Set `PORT=11434` in Railway variables

### Issue: Ollama Not Starting

**Symptom:** No "Ollama is ready!" in logs

**Fix:** Check logs for errors, verify disk space, check model exists

### Issue: Wrong Port

**Symptom:** Ollama listening on 11434 but PORT=8080

**Fix:** Either:
- Set `PORT=11434` in Railway
- Or let Railway set PORT and Ollama will auto-use it

---

## Quick Fix Summary

```bash
# 1. Set PORT in Railway Ollama service
PORT=11434

# 2. Redeploy Ollama service

# 3. Verify in logs:
#    OLLAMA_HOST: 0.0.0.0:11434
#    Ollama is ready!

# 4. Test:
curl https://your-ollama.railway.app/api/tags
```

---

## Still Not Working?

1. **Use Internal URL**: `http://ollama.railway.internal:11434` (works even if external doesn't)
2. **Check Service Status**: Must be "Running" (green)
3. **Check Health Check**: Should be passing
4. **Review Logs**: Look for startup errors

