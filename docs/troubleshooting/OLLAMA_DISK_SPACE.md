# Ollama Disk Space Issues

## Problem

Ollama 502 errors can be caused by **disk space issues** when pulling models. Models are large (~4GB for llama3.1), and Railway services have limited disk space.

## Solution

### 1. Add Railway Volume

**Most Important:** Add a Railway volume to persist models and avoid re-downloading:

1. Go to Ollama service → **Settings** → **Volumes**
2. Click **"Add Volume"**
3. Set:
   - **Mount Path**: `/root/.ollama`
   - **Volume Name**: `ollama-models`
   - **Size**: Start with **20GB** (models are ~4GB each)

This ensures:
- Models persist across deployments
- No need to re-download on every restart
- More reliable storage

### 2. Check Disk Space

If you get disk space errors:

```bash
# SSH into Ollama service
railway shell --service ollama

# Check disk usage
df -h

# Check Ollama directory size
du -sh /root/.ollama
```

### 3. Clean Up if Needed

```bash
# List models
ollama list

# Remove unused models (if you have multiple)
ollama rm <model-name>

# Check space again
df -h
```

### 4. Increase Volume Size

If volume is full:

1. Go to Ollama service → **Settings** → **Volumes**
2. Edit volume
3. Increase size (e.g., 20GB → 50GB)
4. Redeploy service

## Prevention

### Always Use Railway Volumes

**Without volume:**
- Models stored in container filesystem
- Limited space (usually ~10GB)
- Models lost on restart
- Need to re-download every time

**With volume:**
- Models stored in persistent volume
- Can be larger (20GB+)
- Models persist across restarts
- No re-download needed

## Entrypoint Script

The entrypoint script now:
- ✅ Starts Ollama
- ✅ Waits for it to be ready
- ✅ Pulls model if missing
- ✅ Shows helpful error if pull fails (mentions disk space)
- ✅ Keeps container alive with `wait`

**Note:** If disk space was the issue, the simplified script should work fine. The complex monitoring loop was unnecessary.

## Troubleshooting

### "No space left on device"

**Fix:**
1. Add Railway volume (see above)
2. Increase volume size
3. Remove unused models
4. Redeploy service

### Model Pull Fails

**Check:**
```bash
railway logs --service ollama
```

**Look for:**
- "No space left on device"
- "Disk quota exceeded"
- "ENOSPC" errors

**Solution:** Add/increase Railway volume

### 502 Error After Fix

If you still get 502 after fixing disk space:

1. **Check service is running:**
   ```bash
   railway status
   ```

2. **Check logs:**
   ```bash
   railway logs --service ollama
   ```

3. **Test endpoint:**
   ```bash
   curl https://your-ollama.railway.app/api/tags
   ```

4. **Verify volume is mounted:**
   ```bash
   railway shell --service ollama
   df -h /root/.ollama
   ```

## Summary

**Root Cause:** Disk space issues when pulling models

**Solution:**
1. ✅ Add Railway volume at `/root/.ollama` (20GB+)
2. ✅ Simplified entrypoint script (removed complex monitoring)
3. ✅ Better error messages for disk space issues

The entrypoint script is now simpler and focuses on the actual functionality (starting Ollama and pulling models) rather than complex process monitoring.

