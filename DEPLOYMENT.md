# Deployment Guide

## Overview

This guide covers deploying the Agentic Financial Advisor system using **100% free services** where possible, including Ollama (free LLM) for both development and production.

## Deployment Options

### Option 1: Railway (Recommended - Free Tier Available)

Railway offers a free tier that's perfect for this project.

#### Backend Deployment

1. **Create Railway Account**
   - Go to [railway.app](https://railway.app)
   - Sign up with GitHub

2. **Deploy Backend**
   ```bash
   # Install Railway CLI
   npm i -g @railway/cli
   
   # Login
   railway login
   
   # Initialize project
   railway init
   
   # Deploy
   railway up
   ```

3. **Add PostgreSQL**
   - In Railway dashboard, click "New" → "Database" → "PostgreSQL"
   - Railway automatically sets `DATABASE_URL` environment variable

4. **Deploy Ollama Service** (FREE!)
   
   **Step 4a: Create Ollama Service**
   - In Railway dashboard, click "New" → "Empty Service"
   - Name it "ollama" or "ollama-service"
   
   **Step 4b: Add Dockerfile**
   - In the Ollama service, go to Settings → Source
   - Create a new file `Dockerfile` with this content:
   ```dockerfile
   FROM ollama/ollama:latest
   
   EXPOSE 11434
   
   ENV OLLAMA_HOST=0.0.0.0:11434
   
   CMD ["ollama", "serve"]
   ```
   - Or use the provided `Dockerfile.ollama` from the project
   
   **Step 4c: Deploy and Get URL**
   - Railway will automatically build and deploy
   - Once deployed, copy the public URL (e.g., `https://ollama-production-xxxx.up.railway.app`)
   - This will be your `LANGCHAIN4J_OLLAMA_BASE_URL`

5. **Pull Model in Ollama Service**
   ```bash
   # Connect to Ollama service via Railway CLI
   railway link  # Link to your project
   railway shell --service ollama  # Connect to Ollama service
   
   # Pull the model (this may take a few minutes)
   ollama pull llama3.1
   
   # Verify model is available
   ollama list
   ```

6. **Set Environment Variables for Backend**
   ```bash
   # Link to backend service
   railway link --service backend
   
   # Set environment variables
   railway variables set LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
   railway variables set LANGCHAIN4J_OLLAMA_MODEL=llama3.1
   railway variables set ALPHA_VANTAGE_API_KEY=your_key_here
   railway variables set PORT=8080
   ```
   
   **Important**: Replace `https://your-ollama-service.railway.app` with your actual Ollama service URL from step 4c.

#### Frontend Deployment

1. **Build Frontend**
   ```bash
   cd frontend
   npm run build
   ```

2. **Deploy to Railway**
   - Create new service
   - Point to `frontend/dist` directory
   - Set build command: `npm run build`
   - Set start command: `npx serve -s dist`

### Option 2: Render (Free Tier Available)

#### Backend on Render

1. **Create Web Service**
   - Connect GitHub repository
   - Build Command: `cd backend && mvn clean install -DskipTests`
   - Start Command: `cd backend && java -jar target/financialadvisor-0.0.1-SNAPSHOT.jar`

2. **Add PostgreSQL**
   - Create PostgreSQL database in Render dashboard
   - Copy connection string to `DATABASE_URL`

3. **Deploy Ollama**
   - Create a new Web Service
   - Use Docker: `ollama/ollama:latest`
   - Expose port 11434
   - Pull model: `ollama pull llama3.1`

4. **Environment Variables**
   ```
   LANGCHAIN4J_PROVIDER=ollama
   LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama.onrender.com
   LANGCHAIN4J_OLLAMA_MODEL=llama3.1
   ALPHA_VANTAGE_API_KEY=your_key
   ```

### Option 3: Fly.io (Free Tier Available)

#### Deploy Ollama on Fly.io

```bash
# Install flyctl
curl -L https://fly.io/install.sh | sh

# Create Ollama app
fly launch --name your-ollama-app

# Deploy
fly deploy
```

#### Deploy Backend

```bash
cd backend
fly launch
fly deploy
```

## Ollama Deployment Strategies

### Strategy 1: Separate Ollama Service on Railway (Recommended)

Deploy Ollama as a separate service on Railway:

**Steps:**
1. Create new Railway service named "ollama"
2. Add `Dockerfile`:
   ```dockerfile
   FROM ollama/ollama:latest
   EXPOSE 11434
   ENV OLLAMA_HOST=0.0.0.0:11434
   CMD ["ollama", "serve"]
   ```
3. Deploy and get the public URL
4. Pull model: `railway shell --service ollama` then `ollama pull llama3.1`
5. Set backend env var: `LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama.railway.app`

**Docker Compose (for local/production):**
```yaml
version: '3.8'
services:
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_HOST=0.0.0.0:11434

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama:11434
    depends_on:
      - ollama

volumes:
  ollama_data:
```

### Strategy 2: Ollama Cloud Services

Some cloud providers offer managed Ollama:

- **Ollama Cloud** (if available)
- **Hugging Face Spaces** (can host Ollama)
- **Replicate** (Ollama API)

## Environment Variables

### Required Variables

```bash
# Database
DATABASE_URL=postgresql://user:pass@host:5432/dbname
# or
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/dbname
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=pass

# LLM (Ollama - FREE!)
LANGCHAIN4J_PROVIDER=ollama
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7

# Market Data
ALPHA_VANTAGE_API_KEY=your_api_key_here

# Server
PORT=8080
```

### Optional Variables

```bash
# CORS (if frontend on different domain)
CORS_ORIGINS=https://your-frontend.railway.app

# Ollama (deployed on Railway)
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7
```

## Railway-Specific Setup

### Deploy Ollama on Railway

1. **Create New Service**
   - Click "New" → "Empty Service"

2. **Add Dockerfile**
   ```dockerfile
   FROM ollama/ollama:latest
   
   EXPOSE 11434
   
   ENV OLLAMA_HOST=0.0.0.0:11434
   
   CMD ["ollama", "serve"]
   ```

3. **Deploy and Pull Model**
   ```bash
   # After deployment, SSH into service
   railway shell
   
   # Pull model
   ollama pull llama3.1
   ```

4. **Get Service URL**
   - Railway provides a public URL
   - Use this as `LANGCHAIN4J_OLLAMA_BASE_URL`

### Railway Environment Variables

Set these in Railway dashboard:

```
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7
DATABASE_URL=<auto-set by Railway PostgreSQL>
ALPHA_VANTAGE_API_KEY=your_key
PORT=8080
```

## Cost Comparison

### Using Ollama (FREE)
- ✅ **Ollama**: Free (open-source)
- ✅ **Railway Free Tier**: $5 credit/month
- ✅ **Total**: ~$0/month (within free tier limits)

### Using Ollama on Railway (FREE!)
- ✅ **Ollama**: Completely free (open-source)
- ✅ **Railway Free Tier**: $5 credit/month
- ✅ **Total**: ~$0/month (within free tier limits)

**Recommendation**: Deploy Ollama on Railway - it's completely free!

## Production Checklist

- [ ] Deploy Ollama service
- [ ] Pull required model (`llama3.1`)
- [ ] Deploy PostgreSQL database
- [ ] Set all environment variables
- [ ] Deploy backend service
- [ ] Deploy frontend (static build)
- [ ] Configure CORS if needed
- [ ] Set up monitoring/logging
- [ ] Configure rate limiting
- [ ] Test all endpoints
- [ ] Verify WebSocket connections

## Troubleshooting

### Ollama Connection Issues

```bash
# Test Ollama connection
curl http://your-ollama-service.railway.app/api/tags

# Should return list of models
```

### Model Not Found

```bash
# SSH into Ollama service
railway shell

# Pull model
ollama pull llama3.1

# Verify
ollama list
```

### Database Connection

```bash
# Test database connection
railway run psql $DATABASE_URL -c "SELECT 1;"
```

## Monitoring

### Railway Metrics

- CPU usage
- Memory usage
- Network traffic
- Request logs

### Application Logs

```bash
# View logs
railway logs

# Follow logs
railway logs --follow
```

## Scaling Considerations

### Ollama Scaling

- Ollama can handle multiple concurrent requests
- For high traffic, consider:
  - Multiple Ollama instances (load balanced)
  - Larger instance sizes
  - Model caching

### Backend Scaling

- Railway auto-scales based on traffic
- Consider:
  - Database connection pooling
  - Redis for caching (optional)
  - CDN for frontend

## Security

1. **Environment Variables**: Never commit API keys
2. **Database**: Use strong passwords
3. **CORS**: Restrict to your frontend domain
4. **Rate Limiting**: Implement for production
5. **HTTPS**: Railway provides automatically

## Backup Strategy

1. **Database Backups**
   - Railway PostgreSQL: Automatic daily backups
   - Manual backup: `pg_dump $DATABASE_URL > backup.sql`

2. **Ollama Models**
   - Models persist in container volume
   - Re-pull if needed: `ollama pull llama3.1`

---

**Remember**: Ollama is completely free! Deploy it on Railway and you're ready to go.

For more details, see [README.md](./README.md) and [ARCHITECTURE.md](./ARCHITECTURE.md).

