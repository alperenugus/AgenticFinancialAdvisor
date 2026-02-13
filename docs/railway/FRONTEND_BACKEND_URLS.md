# Frontend vs Backend URLs - Critical Guide

## The Problem

You're seeing this error:
```
GET https://agenticfinancialadvisorbackend.railway.internal/advisor/recommendations/...
net::ERR_NAME_NOT_RESOLVED
```

## The Solution

**Frontend MUST use external/public URLs, NOT internal URLs.**

---

## Quick Fix

### In Railway Dashboard → Frontend Service → Variables:

**Change:**
```bash
# ❌ WRONG (causes ERR_NAME_NOT_RESOLVED)
VITE_API_BASE_URL=http://backend.railway.internal:8080/api
VITE_WS_URL=http://backend.railway.internal:8080/ws
```

**To:**
```bash
# ✅ CORRECT (works from browser)
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_URL=https://your-backend.railway.app/ws
```

**Replace `your-backend.railway.app` with your actual backend public URL!**

---

## Understanding Internal vs External URLs

### Internal URLs (`*.railway.internal`)
- ✅ **Work for:** Service-to-service communication within Railway
- ❌ **Don't work for:** Browser requests (frontend)
- **Example:** `http://backend.railway.internal:8080`
- **Use case:** Backend connecting to Ollama, Backend connecting to PostgreSQL

### External URLs (`*.railway.app`)
- ✅ **Work for:** Browser requests (frontend), external API calls
- ✅ **Work for:** Service-to-service (but slower than internal)
- **Example:** `https://backend.railway.app`
- **Use case:** Frontend connecting to Backend, external tools connecting to your API

---

## Configuration Rules

### Backend → Ollama
```bash
# ✅ Can use internal (recommended - faster)
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama.railway.internal:11434

# ✅ Can use external (works but may have 502 errors)
LANGCHAIN4J_OLLAMA_BASE_URL=https://ollama.railway.app
```

### Frontend → Backend
```bash
# ❌ CANNOT use internal (browser can't access)
VITE_API_BASE_URL=http://backend.railway.internal:8080/api

# ✅ MUST use external (browser can access)
VITE_API_BASE_URL=https://backend.railway.app/api
VITE_WS_URL=https://backend.railway.app/ws
```

---

## Finding Your URLs

### In Railway Dashboard

1. Go to your **Backend** service
2. Click **Settings** → **Networking**
3. Copy the **Public Domain** URL
4. Use this in Frontend environment variables

**Example:**
- Backend Public Domain: `https://backend-production-xxxx.up.railway.app`
- Frontend Variables:
  ```bash
  VITE_API_BASE_URL=https://backend-production-xxxx.up.railway.app/api
  VITE_WS_URL=https://backend-production-xxxx.up.railway.app/ws
  ```

---

## Common Errors

### Error: `ERR_NAME_NOT_RESOLVED`
**Cause:** Frontend is using internal URL  
**Fix:** Change to external URL (see Quick Fix above)

### Error: `Network Error` or `CORS Error`
**Cause:** Backend URL is wrong or CORS not configured  
**Fix:** 
1. Verify backend URL is correct
2. Check backend CORS settings include frontend URL

### Error: `502 Bad Gateway`
**Cause:** Backend service is down or health check failing  
**Fix:** Check backend service logs and health status

---

## Summary

| Service | Connects To | URL Type | Example |
|---------|-------------|----------|---------|
| Frontend | Backend | **External** (required) | `https://backend.railway.app` |
| Backend | Ollama | Internal (recommended) | `http://ollama.railway.internal:11434` |
| Backend | PostgreSQL | Internal (auto-set) | `postgresql://...railway.internal...` |

**Remember:** Frontend = External URLs only! 🎯

