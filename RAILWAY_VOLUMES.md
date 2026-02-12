# Railway Volumes Setup for Ollama

Railway doesn't allow `VOLUME` keyword in Dockerfiles. Instead, you need to configure volumes in the Railway dashboard.

---

## Why Volumes Are Needed

Ollama stores downloaded models in `/root/.ollama`. Without a volume:
- Models are lost when the container restarts
- You have to re-download models every time (~4GB download)

With a Railway volume:
- Models persist across deployments
- Faster restarts (no re-download)
- Models survive container updates

---

## Step-by-Step: Add Volume to Ollama Service

### 1. Go to Ollama Service

1. Open your Railway project
2. Click on your **Ollama** service

### 2. Add Volume

1. Go to **Settings** tab
2. Scroll down to **"Volumes"** section
3. Click **"Add Volume"** or **"Create Volume"**
4. Configure:
   - **Mount Path**: `/root/.ollama`
   - **Volume Name**: `ollama-models` (or any name you prefer)
   - **Size**: Start with 10GB (models are ~4GB each, you can increase later)

### 3. Save and Redeploy

1. Click **Save**
2. Railway will automatically:
   - Create the volume
   - Mount it to `/root/.ollama`
   - Redeploy the service

---

## Visual Guide

```
Ollama Service Settings:
├── Settings → Volumes
│   └── Add Volume
│       ├── Mount Path: /root/.ollama
│       ├── Volume Name: ollama-models
│       └── Size: 10GB
│
└── After adding:
    └── Volume mounted at /root/.ollama
    └── Models persist across restarts
```

---

## Verify Volume is Working

### After Adding Volume:

1. **Deploy the service** (if not auto-deployed)
2. **SSH into the service**:
   ```bash
   railway shell --service ollama
   ```
3. **Check if volume is mounted**:
   ```bash
   df -h /root/.ollama
   # Should show the volume size
   ```
4. **Pull a model**:
   ```bash
   ollama pull llama3.1
   ```
5. **Verify model persists**:
   ```bash
   ollama list
   # Should show llama3.1
   ```
6. **Restart the service** (in Railway dashboard)
7. **Check again**:
   ```bash
   railway shell --service ollama
   ollama list
   # Model should still be there!
   ```

---

## Volume Configuration Details

### Mount Path

- **Path**: `/root/.ollama`
- This is where Ollama stores all models by default
- Railway will mount the volume at this location

### Volume Size

- **Minimum**: 10GB (for one model ~4GB)
- **Recommended**: 20GB (for multiple models)
- **Maximum**: Depends on your Railway plan
- You can increase size later if needed

### Volume Name

- Choose any name: `ollama-models`, `ollama-data`, etc.
- This is just for your reference in Railway dashboard

---

## Alternative: Using Railway CLI

If you prefer CLI:

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link to project
railway link

# Create volume (if CLI supports it)
# Note: Railway CLI volume commands may vary
# Check: railway volumes --help
```

**Note**: Volume creation is usually done via dashboard. CLI support may be limited.

---

## Troubleshooting

### Volume Not Mounting

1. **Check Settings**:
   - Go to Ollama service → Settings → Volumes
   - Verify volume is listed
   - Check mount path is `/root/.ollama`

2. **Redeploy Service**:
   - Sometimes volume mounts only apply after redeploy
   - Go to Deployments → Redeploy

3. **Check Logs**:
   ```bash
   railway logs --service ollama
   # Look for volume mount errors
   ```

### Models Still Lost After Restart

1. **Verify Volume**:
   ```bash
   railway shell --service ollama
   ls -la /root/.ollama
   # Should show files if volume is mounted
   ```

2. **Check Volume Size**:
   - Models might not fit if volume is too small
   - Increase volume size in Settings → Volumes

3. **Check Disk Usage**:
   ```bash
   df -h /root/.ollama
   # Verify volume has space
   ```

### Volume Full

1. **Check Usage**:
   ```bash
   railway shell --service ollama
   du -sh /root/.ollama/*
   # See which models take space
   ```

2. **Remove Old Models**:
   ```bash
   ollama rm <model-name>
   ```

3. **Increase Volume Size**:
   - Settings → Volumes → Edit → Increase size

---

## Cost Considerations

### Volume Pricing

- Railway volumes are billed per GB-month
- Check Railway pricing for current rates
- Typically: ~$0.10-0.25 per GB/month

### Example Costs

- **10GB volume**: ~$1-2.50/month
- **20GB volume**: ~$2-5/month
- **50GB volume**: ~$5-12.50/month

**Note**: This is in addition to service compute costs.

---

## Best Practices

1. **Start Small**: Begin with 10GB, increase as needed
2. **Monitor Usage**: Check volume size regularly
3. **Clean Up**: Remove unused models to save space
4. **Backup**: Important models (though Railway volumes are persistent)

---

## Summary

**Steps to Add Volume:**

1. ✅ Go to Ollama service → Settings → Volumes
2. ✅ Click "Add Volume"
3. ✅ Set Mount Path: `/root/.ollama`
4. ✅ Set Size: 10GB (minimum)
5. ✅ Save and redeploy
6. ✅ Pull models - they'll persist now!

**Important**: 
- ❌ Don't use `VOLUME` keyword in Dockerfile
- ✅ Use Railway dashboard to create volumes
- ✅ Mount path must be `/root/.ollama` for Ollama

---

For more details, see Railway documentation: https://docs.railway.com/reference/volumes

