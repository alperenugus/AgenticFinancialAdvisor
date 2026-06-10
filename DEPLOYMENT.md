# Deployment Guide

## Overview

This guide covers deploying the Agentic Financial Advisor system. The system uses **OpenAI API** for fast, cost-effective LLM inference with gpt-4o model.

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

4. **Set Environment Variables for Backend**
   ```bash
   # Link to backend service
   railway link --service backend
   
   # Set required environment variables
   railway variables set OPENAI_API_KEY=your_openai_api_key_here
   railway variables set FINNHUB_API_KEY=your_finnhub_api_key_here
   railway variables set GOOGLE_CLIENT_ID=your_google_client_id
   railway variables set GOOGLE_CLIENT_SECRET=your_google_client_secret
   railway variables set JWT_SECRET=$(openssl rand -base64 32)
   railway variables set CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173
   railway variables set GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google
   railway variables set PORT=8080
   ```
   
   **Important**: 
   - Get your OpenAI API key from [OpenAI Platform](https://platform.openai.com/api-keys)
   - Get your Finnhub API key from [Finnhub](https://finnhub.io/)
   - See [Environment Variables Guide](./docs/ENVIRONMENT_VARIABLES.md) for complete list

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

3. **Set Environment Variables**
   ```
   OPENAI_API_KEY=your_openai_api_key
   FINNHUB_API_KEY=your_finnhub_api_key
   GOOGLE_CLIENT_ID=your_google_client_id
   GOOGLE_CLIENT_SECRET=your_google_client_secret
   JWT_SECRET=your-secure-random-secret
   CORS_ORIGINS=https://your-frontend.onrender.com
   ```

## Environment Variables

### Required Variables

```bash
# Database (Auto-set by Railway PostgreSQL)
DATABASE_URL=postgresql://user:pass@host:5432/dbname

# OpenAI API (LLM Provider)
OPENAI_API_KEY=your_openai_api_key_here

# Market Data
FINNHUB_API_KEY=your_finnhub_api_key_here

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google

# JWT Authentication — REQUIRED. The app FAILS FAST at startup if this is unset, < 32 chars,
# or the old built-in placeholder. Generate one with: openssl rand -base64 48
JWT_SECRET=<run: openssl rand -base64 48>

# CORS (Required if frontend on different domain)
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173

# Market data quote cache TTL in seconds (optional, default 15)
MARKET_DATA_QUOTE_CACHE_TTL_SECONDS=15

# Server
PORT=8080
```

### Optional Variables

```bash
# OpenAI Model Configuration (defaults shown)
OPENAI_ORCHESTRATOR_MODEL=gpt-4o
OPENAI_ORCHESTRATOR_TEMPERATURE=0.0
OPENAI_ORCHESTRATOR_TIMEOUT_SECONDS=90

# Web Search APIs (Optional but recommended)
TAVILY_API_KEY=your_tavily_api_key
SERPER_API_KEY=your_serper_api_key

# News API (Optional)
NEWS_API_KEY=your_news_api_key
```

For complete environment variable documentation, see [ENVIRONMENT_VARIABLES.md](./docs/ENVIRONMENT_VARIABLES.md).

## Railway-Specific Setup

### Railway Environment Variables

Set these in Railway dashboard for your backend service:

```
# Database (Auto-set by Railway PostgreSQL)
DATABASE_URL=<auto-set by Railway>

# OpenAI API (Required)
OPENAI_API_KEY=your_openai_api_key_here

# Market Data (Required)
FINNHUB_API_KEY=your_finnhub_api_key_here

# Google OAuth2 (Required)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google

# JWT (Required)
JWT_SECRET=your-secure-random-secret-key-minimum-32-characters

# CORS (Required)
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173

# Server
PORT=8080
```

For frontend service:

```
VITE_API_BASE_URL=https://your-backend.railway.app/api
VITE_WS_URL=https://your-backend.railway.app/ws
```

## Cost Considerations

### OpenAI API Pricing
- **Free Tier**: Limited requests (check current limits at [OpenAI Platform](https://platform.openai.com/api-keys))
- **Paid Tier**: Pay-per-use pricing, very cost-effective for LLM inference
- **Model**: gpt-4o provides excellent quality at competitive pricing

### Railway Pricing
- **Free Tier**: $5 credit/month
- **PostgreSQL**: Included in free tier (with limits)
- **Total**: Very affordable for small to medium deployments

## Production Checklist

- [ ] Deploy PostgreSQL database (Railway auto-sets `DATABASE_URL`)
- [ ] Get OpenAI API key from [OpenAI Platform](https://platform.openai.com/api-keys)
- [ ] Get Finnhub API key from [Finnhub](https://finnhub.io/)
- [ ] Set up Google OAuth2 credentials (see [Google Auth Setup](./docs/GOOGLE_AUTH_SETUP.md))
- [ ] Set all required environment variables
- [ ] Deploy backend service
- [ ] Deploy frontend (static build)
- [ ] Configure CORS with frontend URL
- [ ] Test authentication flow
- [ ] Test all API endpoints
- [ ] Verify WebSocket connections
- [ ] Set up monitoring/logging
- [ ] Configure rate limiting (optional but recommended)

## Troubleshooting

### OpenAI API Issues

```bash
# Test OpenAI API connection
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# Should return list of available models
```

### Database Connection

```bash
# Test database connection
railway run psql $DATABASE_URL -c "SELECT 1;"
```

### Authentication Issues

- See [Google Auth Setup Guide](./docs/GOOGLE_AUTH_SETUP.md)
- See [Google Auth Troubleshooting](./docs/GOOGLE_AUTH_TROUBLESHOOTING.md)

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

### Backend Scaling

- Railway auto-scales based on traffic
- Consider:
  - Database connection pooling (HikariCP configured)
  - Redis for caching (optional, for market data)
  - CDN for frontend static assets
  - Load balancing for multiple backend instances

### OpenAI API Scaling

- OpenAI API handles high throughput automatically
- No infrastructure management needed
- Pay-per-use pricing scales with usage

## Security

1. **Environment Variables**: Never commit API keys to version control
2. **Database**: Use strong passwords, Railway auto-generates secure credentials
3. **CORS**: Restrict to your frontend domain
4. **Rate Limiting**: Implemented per-session using token bucket algorithm
5. **HTTPS**: Railway provides automatically
6. **JWT Tokens**: Secure, stateless authentication with expiration

## Backup Strategy

1. **Database Backups**
   - Railway PostgreSQL: Automatic daily backups
   - Manual backup: `pg_dump $DATABASE_URL > backup.sql`

2. **Environment Variables**
   - Export Railway environment variables for backup
   - Store securely (password manager, secrets manager)

---

For more details, see:
- [README.md](./README.md) - Project overview
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture
- [ENVIRONMENT_VARIABLES.md](./docs/ENVIRONMENT_VARIABLES.md) - Complete environment variable guide
- [Railway Deployment Guide](./docs/railway/RAILWAY_GUIDE.md) - Detailed Railway setup

