# Google OAuth2 Troubleshooting Guide

## Common Issues

### Error 400: redirect_uri_mismatch

This error occurs when the redirect URI sent by your application doesn't match what's configured in Google Cloud Console.

#### Symptoms
- Error message: "Error 400: redirect_uri_mismatch"
- The redirect URI in the error shows `http://` instead of `https://`

#### Solution

1. **Fix Google Cloud Console Configuration**

   Go to [Google Cloud Console](https://console.cloud.google.com/) → Your Project → APIs & Services → Credentials → Your OAuth 2.0 Client ID

   In **Authorized redirect URIs**, add:
   ```
   https://agenticfinancialadvisorbackend-production.up.railway.app/login/oauth2/code/google
   ```

   **Important:**
   - Must be **exactly** this format (no trailing slash)
   - Must use **HTTPS** (not HTTP)
   - Must include the full path: `/login/oauth2/code/google`
   - No trailing spaces

2. **Set Environment Variable in Railway**

   In your Railway backend service, set:
   ```
   GOOGLE_REDIRECT_URI=https://agenticfinancialadvisorbackend-production.up.railway.app/login/oauth2/code/google
   ```

   **Important:** 
   - Use your **actual** Railway backend URL
   - Must be **HTTPS**
   - Must include the full path

3. **Verify Configuration**

   After setting the environment variable:
   - Wait for Railway to redeploy
   - Clear your browser cache
   - Try signing in again

#### Why This Happens

Spring Boot OAuth2 automatically constructs the redirect URI using `{baseUrl}/login/oauth2/code/{registrationId}`. When behind a proxy (like Railway), Spring Boot might:
- Detect HTTP instead of HTTPS
- Use the wrong hostname
- Not properly read proxy headers

By explicitly setting `GOOGLE_REDIRECT_URI`, we force Spring Boot to use the correct HTTPS URL.

### Invalid Credentials Error

If you see "Invalid credentials" on the login page, this usually means the `GOOGLE_CLIENT_ID` or `GOOGLE_CLIENT_SECRET` is missing or incorrect.

#### Step 1: Check Railway Environment Variables

1. Go to Railway → Your Backend Service → Variables
2. Verify these variables exist and are set:
   ```
   GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=GOCSPX-your-secret-here
   ```
3. **Important checks:**
   - No quotes around the values (Railway sometimes adds quotes automatically)
   - No leading/trailing spaces
   - Copy the exact values from Google Cloud Console
   - Make sure you're using the **Client ID** (not the Client Secret) for `GOOGLE_CLIENT_ID`
   - Make sure you're using the **Client Secret** (not the Client ID) for `GOOGLE_CLIENT_SECRET`

#### Step 2: Verify in Google Cloud Console

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Navigate to: Your Project → APIs & Services → Credentials
3. Click your OAuth 2.0 Client ID
4. **Copy the Client ID** - should look like: `123456789-abc...xyz.apps.googleusercontent.com`
5. **Click "Show" next to Client Secret** - should look like: `GOCSPX-abc...xyz`
6. Compare with what's in Railway

#### Step 3: Check Railway Logs

After redeploying, check the startup logs:

```bash
railway logs --service backend
```

Look for:
- ✅ `GOOGLE_CLIENT_ID is set: ...` (means it's configured)
- ❌ `GOOGLE_CLIENT_ID is not set or is empty!` (means it's missing)

#### Step 4: Common Mistakes

1. **Swapped values**: Client ID in Secret field, or vice versa
2. **Extra quotes**: Railway might add quotes - remove them
3. **Wrong project**: Using credentials from a different Google Cloud project
4. **OAuth consent screen not configured**: Go to OAuth consent screen and complete setup
5. **API not enabled**: Enable "Google Identity API" or "Google+ API" in Google Cloud Console

#### Step 5: Recreate Credentials (if needed)

If nothing works, create new credentials:

1. In Google Cloud Console → Credentials
2. Delete the old OAuth 2.0 Client ID
3. Create a new one
4. Copy the new Client ID and Secret
5. Update Railway environment variables
6. Redeploy

### Redirect URI Still Shows HTTP

If the error still shows `http://` instead of `https://`:

1. **Check Railway Environment Variables**
   ```bash
   # Verify the variable is set correctly
   railway variables --service backend | grep GOOGLE_REDIRECT_URI
   ```

2. **Verify Proxy Configuration**
   - The `ForwardedHeaderFilter` should be active
   - Check Railway logs for any proxy header issues

3. **Explicit HTTPS Configuration**
   If still not working, you may need to add this to `application.yml`:
   ```yaml
   server:
     forward-headers-strategy: framework
     use-forward-headers: true
   ```

### Testing the Redirect URI

To test what redirect URI Spring Boot is sending:

1. **Enable Debug Logging**
   Add to `application.yml`:
   ```yaml
   logging:
     level:
       org.springframework.security.oauth2: DEBUG
   ```

2. **Check Railway Logs**
   Look for lines like:
   ```
   Redirecting to: https://accounts.google.com/o/oauth2/v2/auth?redirect_uri=...
   ```

3. **Compare with Google Console**
   The `redirect_uri` parameter in the logs should exactly match what's in Google Cloud Console.

## Quick Checklist

- [ ] Google Cloud Console has the full redirect URI: `https://your-backend.railway.app/login/oauth2/code/google`
- [ ] Railway environment variable `GOOGLE_REDIRECT_URI` is set with HTTPS
- [ ] `GOOGLE_CLIENT_ID` is set correctly
- [ ] `GOOGLE_CLIENT_SECRET` is set correctly
- [ ] Application has been redeployed after setting variables
- [ ] Browser cache cleared
- [ ] No trailing spaces in Google Console or Railway variables

## Still Not Working?

1. **Check Railway Logs**
   ```bash
   railway logs --service backend
   ```
   Look for OAuth2-related errors

2. **Verify Database Connection**
   OAuth2 requires database access to store user information

3. **Test Locally First**
   Use `http://localhost:8080/login/oauth2/code/google` in Google Console for local testing

4. **Contact Support**
   If all else fails, check:
   - Railway support for proxy configuration
   - Spring Boot OAuth2 documentation
   - Google OAuth2 documentation

