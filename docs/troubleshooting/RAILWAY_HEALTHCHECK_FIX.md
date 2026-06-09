# Fix: Railway Health Check Marks the Deploy FAILED

## Problem

Railway runs an HTTP health check against the backend after every deploy. If that
request does not return a `2xx`, Railway marks the deployment **FAILED** and keeps
serving the previous (stale) build — even though the new container started fine.

The trap we hit: the health check path was pointed at an **authentication-protected**
endpoint (the old `/api/advisor/status` while it sat behind Spring Security). Spring
Security answers an unauthenticated request to a protected path with a **302 redirect to
Google login** (`/oauth2/authorization/google`). Railway reads that 302 as "service
unavailable" and fails the deploy. Every backend deploy was silently failing this way
while production kept running an old build.

## Root Cause

**The Railway health check path must be PUBLIC.** An auth-protected path 302-redirects to
the OAuth2 login flow, and the redirect is not a healthy response.

## Solution (now in place)

1. **Dedicated public liveness endpoint.** `GET /api/health` returns:
   ```json
   {"status":"UP","service":"financial-advisor","timestamp":"..."}
   ```
2. **Both liveness paths are `permitAll`.** In `SecurityConfig`, `/api/health` and
   `/api/advisor/status` are explicitly public so the health check gets a `200` instead of
   a `302`:
   ```java
   .requestMatchers("/api/health", "/api/advisor/status").permitAll()
   ```
3. **Railway health check path points at `/api/health`** (`backend/railway.toml`).

That is the whole fix. The earlier "increase the timeout" advice was treating a symptom —
the app was starting fine; the check was just being redirected to a login page.

---

## What Was Changed

### backend/src/main/java/com/agent/financialadvisor/config/SecurityConfig.java
- Added `/api/health` and `/api/advisor/status` to the `permitAll` matchers so an
  unauthenticated health check receives a `200`, not a `302` to Google login.

### backend/.../controller (health endpoint)
- `GET /api/health` returns `{"status":"UP","service":"financial-advisor","timestamp":...}`.

### backend/railway.toml
- `healthcheckPath = "/api/health"` (a public path).

---

## Required Environment Variable: JWT_SECRET

The backend **fails fast at startup** if a strong `JWT_SECRET` is not set. There is no
hardcoded fallback secret anymore. If `JWT_SECRET` is missing, too short (`< 32` chars), or
still the old placeholder, the app will not boot — and the health check will then
(correctly) fail because nothing is listening.

Generate one and set it on the backend service:

```bash
openssl rand -base64 48
```

```bash
# Railway → Backend service → Variables
JWT_SECRET=<paste the generated value>
```

Local and test profiles supply their own dev secret, so this is a Railway-only requirement.

---

## Verify the Fix

After deploy, the health check path must return `200` with no redirect:

```bash
# Should return {"status":"UP",...} with HTTP 200 (NOT a 302)
curl -i https://agenticfinancialadvisorbackend-production.up.railway.app/api/health
```

```bash
# Also public — the agent status payload
curl https://agenticfinancialadvisorbackend-production.up.railway.app/api/advisor/status
```

If you see `HTTP/1.1 302 Found` with a `Location:` header pointing at
`/oauth2/authorization/google`, the path is still protected — re-check the `permitAll`
matchers in `SecurityConfig`.

---

## If the Health Check Still Fails

Now that the path is public, a failing check almost always means the app did not start.
Check the logs:

```bash
railway logs --service backend
```

Common startup blockers:

1. **`JWT_SECRET` missing / weak** → app fails fast at boot. Set a strong value (see above).
2. **`DATABASE_URL` missing or wrong** → verify the PostgreSQL service is running and linked.
3. **Bean creation / context load failure** → read the stack trace; fix the misconfigured bean.

Confirm a clean start with:

```
Started FinancialAdvisorApplication in N seconds
```

---

## Summary

The health check was never a timing problem — it was an **auth** problem. The Railway
health check path must be public. `/api/health` (and `/api/advisor/status`) are now
`permitAll`, the check returns `200`, and the deploy is marked SUCCESS. Don't forget
`JWT_SECRET`, or the app won't boot at all.
