# Google Sign-In Integration Guide

## Overview

The application now uses Google OAuth2 for authentication. Users must sign in with their Google account to access the application.

## Backend Setup

### 1. Google OAuth2 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Configure:
   - Application type: Web application
   - Authorized redirect URIs:
     - `http://localhost:8080/login/oauth2/code/google` (local)
     - `https://your-backend.railway.app/login/oauth2/code/google` (production)
6. Copy the Client ID and Client Secret

### 2. Environment Variables (Railway)

Set these in your Railway backend service:

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id_here
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
GOOGLE_REDIRECT_URI=https://your-backend.railway.app/login/oauth2/code/google

# JWT Secret (generate a secure random string, minimum 32 characters)
JWT_SECRET=your-secure-random-secret-key-minimum-32-characters-long
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# CORS Origins (include your frontend URL)
CORS_ORIGINS=https://your-frontend.railway.app,http://localhost:5173
```

### 3. Database

The `User` table will be automatically created. It stores:
- Email (unique identifier)
- Name
- Google ID
- Profile picture URL

## Frontend Setup

### 1. Environment Variables

Set in Railway frontend service or `.env`:

```bash
VITE_API_BASE_URL=https://your-backend.railway.app/api
```

### 2. OAuth Flow

1. User clicks "Sign in with Google"
2. Redirects to backend: `/oauth2/authorization/google`
3. Backend handles Google OAuth2 flow
4. On success, redirects to frontend: `/auth/callback#token=JWT_TOKEN` (URL fragment, not query parameter)
5. Frontend stores token and loads user data

## API Changes

### Updated Endpoints

All endpoints now require authentication (JWT token in `Authorization: Bearer <token>` header):

- `GET /api/profile` - Get current user's profile (no userId in path)
- `POST /api/profile` - Create profile (no userId in body)
- `PUT /api/profile` - Update profile (no userId in path)
- `GET /api/portfolio` - Get portfolio (no userId in path)
- `POST /api/portfolio/holdings` - Add holding (no userId in path)
- `DELETE /api/portfolio/holdings/{holdingId}` - Remove holding
- `POST /api/portfolio/refresh` - Refresh prices
- `POST /api/advisor/analyze` - Analyze query (no userId in body)
- `GET /api/advisor/recommendations` - Get recommendations (no userId in path)

### New Endpoints

- `GET /api/auth/me` - Get current authenticated user
- `POST /api/auth/validate` - Validate JWT token

## Security

- All API endpoints (except `/api/auth/**` and `/oauth2/**`) require authentication
- JWT tokens are stored in localStorage
- Tokens expire after 24 hours (configurable)
- Automatic token refresh on API calls
- 401 errors automatically redirect to login

## Testing Locally

1. Set up Google OAuth2 credentials
2. Add to `application-local.yml` or environment:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: YOUR_CLIENT_ID
               client-secret: YOUR_CLIENT_SECRET
   ```
3. Run backend: `mvn spring-boot:run`
4. Run frontend: `npm run dev`
5. Navigate to `http://localhost:5173`
6. Click "Sign in with Google"

## Troubleshooting

### "Redirect URI mismatch"
- Ensure redirect URI in Google Console matches exactly: `http://localhost:8080/login/oauth2/code/google`

### "401 Unauthorized"
- Check that JWT token is being sent in `Authorization` header
- Verify token hasn't expired
- Check `JWT_SECRET` matches between requests

### "User not authenticated"
- Ensure user completed Google OAuth flow
- Check that token is stored in localStorage
- Verify backend security configuration

