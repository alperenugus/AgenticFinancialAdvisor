# Railway Health Check Failure - Troubleshooting Guide

## Common Reasons Health Check Fails

### 1. Application Not Starting (Most Common)

**Symptoms:**
- Health check times out
- No response from `/api/advisor/status`

**Check logs:**
```bash
railway logs --service backend
```

**Common causes:**

#### A. Database Connection Failed
```
Error: Connection to postgresql://... refused
```

**Fix:**
- Verify PostgreSQL service is running in Railway
- Check `DATABASE_URL` environment variable exists
- Test connection: `railway run psql $DATABASE_URL -c "SELECT 1;"`

#### B. Missing Environment Variables
```
Error: Required property 'LANGCHAIN4J_OLLAMA_BASE_URL' not found
```

**Fix:**
- Set all required environment variables in Railway dashboard
- See [ENVIRONMENT_VARIABLES.md](./ENVIRONMENT_VARIABLES.md)

#### C. Application Crashes on Startup
```
Error: Bean creation failed
Exception in thread "main"
```

**Fix:**
- Check logs for specific error
- Verify all dependencies are correct
- Check if Ollama is blocking startup (see #2)

---

### 2. Ollama Connection Blocking Startup

**Symptoms:**
- Application hangs during startup
- Logs show "Connecting to Ollama..."
- Health check times out

**Problem:**
If `LangChain4jConfig` tries to connect to Ollama during bean initialization, and Ollama is unreachable, it can block startup.

**Check:**
```bash
# Test Ollama connection
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

**Fix:**
- Verify Ollama service is running
- Check `LANGCHAIN4J_OLLAMA_BASE_URL` is correct (no `:8080`)
- Make Ollama connection lazy (see solution below)

---

### 3. Application Takes Too Long to Start

**Symptoms:**
- Application starts but health check times out before it's ready
- Logs show "Started FinancialAdvisorApplication" but health check fails

**Current Settings:**
- Railway timeout: 300 seconds (5 minutes)
- Docker health check start period: 180 seconds (3 minutes)

**If still timing out:**
- Increase timeout further (see below)
- Check if database migrations are slow
- Check if JPA initialization is slow

---

### 4. Port Binding Issues

**Symptoms:**
- Application starts but can't bind to port
- "Port already in use" errors

**Fix:**
- Railway sets `PORT` automatically - don't override it
- Make sure `application.yml` uses `${PORT:8080}`

---

### 5. Health Check Endpoint Not Accessible

**Symptoms:**
- Application starts successfully
- Health check returns 404 or connection refused

**Check:**
```bash
# Test manually after deployment
curl https://your-backend.railway.app/api/advisor/status
```

**Fix:**
- Verify endpoint exists: `GET /api/advisor/status`
- Check if CORS is blocking
- Verify health check path in `railway.toml`: `/api/advisor/status`

---

## Step-by-Step Debugging

### Step 1: Check Railway Logs

```bash
railway logs --service backend --follow
```

**Look for:**
- ✅ "Started FinancialAdvisorApplication" = App started successfully
- ❌ "Application run failed" = App crashed
- ❌ "Connection refused" = Database/Ollama connection issue
- ❌ "Bean creation failed" = Configuration error

### Step 2: Verify Environment Variables

In Railway dashboard → Backend service → Variables:

**Required:**
- [ ] `DATABASE_URL` (auto-set by PostgreSQL)
- [ ] `LANGCHAIN4J_OLLAMA_BASE_URL`
- [ ] `LANGCHAIN4J_OLLAMA_MODEL=llama3.1`
- [ ] `PORT` (auto-set by Railway)

**Recommended:**
- [ ] `ALPHA_VANTAGE_API_KEY`

### Step 3: Test Services Manually

**Test Database:**
```bash
railway run psql $DATABASE_URL -c "SELECT 1;"
```

**Test Ollama:**
```bash
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags
```

**Test Backend (after deployment):**
```bash
curl https://your-backend.railway.app/api/advisor/status
```

### Step 4: Check Service Status

In Railway dashboard:
- [ ] PostgreSQL service: **Running** (green)
- [ ] Ollama service: **Running** (green)
- [ ] Backend service: **Deploying** or **Running**

---

## Solutions

### Solution 1: Make Ollama Connection Lazy

If Ollama connection is blocking startup, make it lazy:

**backend/src/main/java/com/agent/financialadvisor/config/LangChain4jConfig.java:**

```java
@Lazy  // Add this annotation
@Configuration
public class LangChain4jConfig {
    // ... existing code
}
```

This delays Ollama connection until first use, not during startup.

### Solution 2: Increase Health Check Timeout

**backend/railway.toml:**
```toml
[deploy]
healthcheckTimeout = 600  # 10 minutes
```

**backend/Dockerfile:**
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=300s --retries=10 \
  CMD curl -f http://localhost:8080/api/advisor/status || exit 1
```

### Solution 3: Disable Health Check Temporarily

To see if app starts without health check:

**backend/railway.toml:**
```toml
[deploy]
# healthcheckPath = "/api/advisor/status"  # Comment out
healthcheckTimeout = 600
```

Or remove health check from Dockerfile temporarily.

### Solution 4: Add Simple Health Endpoint

Create a simpler health endpoint that doesn't require database/Ollama:

**backend/src/main/java/com/agent/financialadvisor/controller/HealthController.java:**
```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return ResponseEntity.ok(status);
    }
}
```

Then update `railway.toml`:
```toml
healthcheckPath = "/health"
```

---

## Quick Diagnostic Commands

```bash
# 1. Check logs
railway logs --service backend --follow

# 2. Check environment variables
railway variables --service backend

# 3. Test database
railway run psql $DATABASE_URL -c "SELECT 1;"

# 4. Test Ollama
curl https://agenticfinancialadvisorollama-production.up.railway.app/api/tags

# 5. Test backend (after it starts)
curl https://your-backend.railway.app/api/advisor/status

# 6. SSH into service
railway shell --service backend
```

---

## Most Likely Issues (Priority Order)

1. **Missing `DATABASE_URL`** - Check PostgreSQL service is running
2. **Ollama connection blocking** - Make connection lazy or verify Ollama is accessible
3. **Application crash** - Check logs for specific error
4. **Timeout too short** - Increase to 600 seconds
5. **Port binding issue** - Verify `PORT` env var is set

---

## Expected Behavior

**Successful Startup:**
```
1. Build completes (3-5 min)
2. Container starts
3. Spring Boot initializes (30-120 sec)
4. Database connects
5. Application context loads
6. "Started FinancialAdvisorApplication" in logs
7. Health check passes (within 5 min)
```

**If any step fails, check logs for that specific step.**

---

## Still Not Working?

1. **Share Railway logs** - `railway logs --service backend`
2. **Check deployment status** - Railway dashboard → Deployments
3. **Verify all services running** - PostgreSQL, Ollama, Backend
4. **Test endpoints manually** - After deployment, test with curl

The logs will tell you exactly what's failing!

