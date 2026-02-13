# Fix: CORS Error - Access-Control-Allow-Origin

## Problem

You're seeing this error in the browser:
```
Access to XMLHttpRequest at 'https://backend.railway.app/api/...' 
from origin 'https://frontend.railway.app' has been blocked by CORS policy: 
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

## Root Cause

The backend CORS configuration has a conflict:
- `allowCredentials(true)` + `allowedOriginPatterns("*")` = **Not allowed by browsers**

Browsers don't allow credentials with wildcard origins for security reasons.

## Solution

### Option 1: Set CORS_ORIGINS Environment Variable (Recommended)

In your **Backend** service on Railway:

1. Go to **Settings** → **Variables**
2. Add/Update:
   ```bash
   CORS_ORIGINS=https://agenticfinancialadvisorfrontend-production.up.railway.app,http://localhost:5173,http://localhost:3000
   ```
3. **Redeploy** the backend service

**Benefits:**
- ✅ Allows credentials (cookies, auth headers)
- ✅ More secure (specific origins only)
- ✅ Works with WebSocket

### Option 2: Use Wildcard (No Credentials)

If you don't need credentials, the default wildcard configuration will work:

```bash
# In Backend service variables, either:
# - Don't set CORS_ORIGINS (uses wildcard *)
# - Or set: CORS_ORIGINS=*
```

**Limitations:**
- ❌ Can't use credentials
- ❌ Less secure
- ✅ Works for basic API calls

---

## Quick Fix Steps

### Step 1: Get Your Frontend URL

1. Go to **Frontend** service in Railway
2. **Settings** → **Networking**
3. Copy the **Public Domain** URL
4. Example: `https://agenticfinancialadvisorfrontend-production.up.railway.app`

### Step 2: Set CORS_ORIGINS in Backend

1. Go to **Backend** service
2. **Settings** → **Variables**
3. Add:
   ```bash
   CORS_ORIGINS=https://agenticfinancialadvisorfrontend-production.up.railway.app,http://localhost:5173,http://localhost:3000
   ```
4. Replace with your actual frontend URL

### Step 3: Redeploy Backend

Railway will auto-redeploy when you save the variable, or manually redeploy.

### Step 4: Test

Open your frontend in browser and check console - CORS error should be gone!

---

## Configuration Details

### Current Configuration

The backend now supports:

1. **Wildcard mode** (if `CORS_ORIGINS=*` or not set):
   - Allows all origins
   - `allowCredentials(false)` (browser requirement)
   - Works for basic API calls

2. **Specific origins mode** (if `CORS_ORIGINS` has specific URLs):
   - Allows only listed origins
   - `allowCredentials(true)` (can use cookies/auth)
   - More secure

### Example CORS_ORIGINS Value

```bash
# Production + Local development
CORS_ORIGINS=https://frontend.railway.app,http://localhost:5173,http://localhost:3000

# Multiple production environments
CORS_ORIGINS=https://frontend.railway.app,https://staging.railway.app,http://localhost:5173
```

---

## Verification

### Check CORS Headers

After setting `CORS_ORIGINS`, test with curl:

```bash
curl -H "Origin: https://your-frontend.railway.app" \
     -H "Access-Control-Request-Method: GET" \
     -X OPTIONS \
     https://your-backend.railway.app/api/advisor/status \
     -v
```

Look for:
```
Access-Control-Allow-Origin: https://your-frontend.railway.app
Access-Control-Allow-Credentials: true
```

### Browser Test

1. Open frontend in browser
2. Open Developer Tools → Network tab
3. Make an API request
4. Check response headers:
   - Should have `Access-Control-Allow-Origin`
   - Should match your frontend URL

---

## Common Issues

### Issue: Still Getting CORS Error

**Check:**
1. `CORS_ORIGINS` is set correctly (no typos in URL)
2. Frontend URL matches exactly (including `https://`)
3. Backend service was redeployed after setting variable
4. No trailing slashes in URLs

### Issue: 404 Error After CORS Fix

**This is a different issue:**
- CORS is fixed, but endpoint doesn't exist
- Check endpoint path matches controller mapping
- Verify backend is running and healthy

### Issue: WebSocket CORS

WebSocket has separate CORS configuration. The fix also updates WebSocket CORS to allow all origins (needed for Railway).

---

## Summary

**Quick Fix:**
```bash
# In Backend Railway Variables:
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173
```

**Also verify Frontend variables:**
```bash
# In Frontend Railway Variables:
VITE_API_BASE_URL=https://your-backend.railway.app/api  # ⚠️ Must include /api
VITE_WS_URL=https://your-backend.railway.app/ws         # ⚠️ Must include /ws
```

**Then redeploy both services.**

The CORS error should be resolved! ✅

---

## Additional Fix: CORS Filter

A `CorsFilter` has been added to handle CORS headers for:
- All REST API endpoints (`/api/**`)
- WebSocket/SockJS endpoints (`/ws/**`, `/ws/info/**`)
- Preflight OPTIONS requests

This ensures CORS headers are sent even if Spring's CORS configuration doesn't catch all cases.

