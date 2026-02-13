# Quick Guide: Deploy Ollama on Railway

## Step-by-Step Instructions

### 1. Create Ollama Service on Railway

1. Go to your Railway project dashboard
2. Click **"New"** → **"Empty Service"**
3. Name it `ollama` or `ollama-service`

### 2. Add Dockerfile

1. In the Ollama service, go to **Settings** → **Source**
2. Click **"Add Dockerfile"** or create a new file
3. Paste this content:

```dockerfile
FROM ollama/ollama:latest

EXPOSE 11434

ENV OLLAMA_HOST=0.0.0.0:11434

CMD ["ollama", "serve"]
```

4. Save the file
5. Railway will automatically detect and deploy

### 3. Wait for Deployment

- Railway will build and deploy the Ollama service
- This usually takes 2-3 minutes
- Once deployed, you'll see a public URL like: `https://ollama-production-xxxx.up.railway.app`
- **Copy this URL** - you'll need it for the backend configuration

### 4. Pull the Model

```bash
# Install Railway CLI (if not already installed)
npm i -g @railway/cli

# Login to Railway
railway login

# Link to your project
railway link

# Connect to the Ollama service
railway shell --service ollama

# Pull the model (this downloads ~4GB, takes 5-10 minutes)
ollama pull llama3.1

# Verify the model is available
ollama list
```

You should see `llama3.1` in the list.

### 5. Configure Backend to Use Ollama

1. Go to your **Backend** service in Railway
2. Go to **Variables** tab
3. Add these environment variables:

```
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7
```

**Important**: Replace `https://your-ollama-service.railway.app` with the actual URL from step 3.

### 6. Test the Connection

After setting the environment variables, restart your backend service. The backend will now connect to your Ollama service on Railway.

## Troubleshooting

### Model Not Found
If you get "model not found" errors:
```bash
railway shell --service ollama
ollama pull llama3.1
```

### Connection Refused
- Check that Ollama service is running (green status in Railway)
- Verify the URL is correct (no trailing slash)
- Make sure `OLLAMA_HOST=0.0.0.0:11434` is set in Ollama service

### Model Download Fails
- Railway free tier has limited resources
- Try a smaller model: `ollama pull llama3.1:8b` (if available)
- Or use `mistral` which is smaller: `ollama pull mistral`

## Cost

- **Ollama**: Completely free (open-source)
- **Railway Free Tier**: $5 credit/month
- **Total**: ~$0/month (within free tier limits)

---

For more details, see [DEPLOYMENT.md](../DEPLOYMENT.md).

