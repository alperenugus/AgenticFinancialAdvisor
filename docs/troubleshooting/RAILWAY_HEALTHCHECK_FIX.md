# Fix: Railway Health Check Timeout

## Problem

Railway health check is failing because Spring Boot takes longer than 1m40s to start, especially when:
- Connecting to database
- Initializing JPA/Hibernate
- Connecting to Ollama
- Loading application context

## Solution

I've made these changes:

1. **Increased Railway health check timeout** to 300 seconds (5 minutes)
2. **Increased Docker health check start period** to 180 seconds (3 minutes)
3. **Added curl** to Alpine image (for health checks)
4. **Increased retries** to 5

---

## What Was Changed

### backend/Dockerfile
- Added `curl` installation (Alpine doesn't have wget)
- Increased health check start period: `180s` (3 minutes)
- Increased timeout: `10s`
- Increased retries: `5`

### backend/railway.toml
- Increased `healthcheckTimeout`: `300` (5 minutes)

---

## Additional Troubleshooting

If health check still fails, check these:

### 1. Check Application Logs

```bash
railway logs --service backend
```

Look for:
- Database connection errors
- Ollama connection errors
- Application startup errors
- Missing environment variables

### 2. Verify Environment Variables

Make sure these are set in Railway:

**Required:**
- `DATABASE_URL` (auto-set by PostgreSQL service)
- `LANGCHAIN4J_OLLAMA_BASE_URL` (your Ollama service URL)
- `LANGCHAIN4J_OLLAMA_MODEL=llama3.1`
- `PORT=8080` (auto-set by Railway)

**Optional but recommended:**
- `ALPHA_VANTAGE_API_KEY`
- `CORS_ORIGINS` (if frontend on different domain)

### 3. Test Health Check Manually

After deployment, test the endpoint:

```bash
# Get your backend URL from Railway
curl https://your-backend.railway.app/api/advisor/status
```

Should return:
```json
{
  "agents": {...},
  "status": "operational"
}
```

### 4. Check Database Connection

If database connection is slow:

1. Verify PostgreSQL service is running
2. Check `DATABASE_URL` is correct
3. Test connection:
   ```bash
   railway run psql $DATABASE_URL -c "SELECT 1;"
   ```

### 5. Check Ollama Connection

If Ollama connection is failing:

1. Verify Ollama service is running
2. Check `LANGCHAIN4J_OLLAMA_BASE_URL` is correct
3. Test connection:
   ```bash
   curl https://your-ollama.railway.app/api/tags
   ```

### 6. Disable Health Check Temporarily

If you want to see if the app starts without health check:

**Option A: Remove from Dockerfile**
- Comment out the `HEALTHCHECK` line
- Redeploy

**Option B: Disable in Railway**
- Settings → Deploy
- Remove or increase `healthcheckTimeout` to very high value
- Or remove `healthcheckPath`

---

## Expected Startup Time

Spring Boot applications typically take:
- **Fast**: 30-60 seconds
- **Normal**: 60-120 seconds
- **Slow**: 120-180 seconds (with database, external services)

With our new settings:
- **Health check starts after**: 180 seconds (3 minutes)
- **Railway timeout**: 300 seconds (5 minutes)
- **Should be enough** for most cases

---

## If Still Failing

### Check Logs for Specific Errors

```bash
railway logs --service backend --follow
```

Common issues:

1. **Database connection timeout**
   - Solution: Check `DATABASE_URL`, verify PostgreSQL is running

2. **Ollama connection timeout**
   - Solution: Check `LANGCHAIN4J_OLLAMA_BASE_URL`, verify Ollama is running

3. **Missing environment variables**
   - Solution: Add all required variables in Railway dashboard

4. **Port binding issues**
   - Solution: Make sure `PORT` environment variable is set (Railway auto-sets this)

5. **Memory issues**
   - Solution: Increase service resources in Railway

### Increase Timeout Further

If 5 minutes isn't enough:

**railway.toml:**
```toml
healthcheckTimeout = 600  # 10 minutes
```

**Dockerfile:**
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=300s --retries=10 \
  CMD curl -f http://localhost:8080/api/advisor/status || exit 1
```

---

## Quick Fix Checklist

- [ ] Health check timeout increased to 300s in `railway.toml`
- [ ] Docker health check start period increased to 180s
- [ ] `curl` installed in Dockerfile
- [ ] All environment variables set in Railway
- [ ] Database service is running
- [ ] Ollama service is running (if required for startup)
- [ ] Check logs for specific errors

---

## Summary

The health check timeout has been increased to give Spring Boot more time to start. The application should now pass health checks within 3-5 minutes of deployment.

If it still fails, check the logs to identify the specific issue (database, Ollama, missing variables, etc.).

