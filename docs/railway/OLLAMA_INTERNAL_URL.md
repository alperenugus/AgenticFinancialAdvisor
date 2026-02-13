# Using Ollama Internal URL on Railway

## Problem

When connecting your backend to Ollama on Railway, you may get 502 errors when using the external URL:
```
https://your-ollama-service.railway.app
```

## Solution: Use Internal Railway URL

Railway services can communicate via **internal URLs** within Railway's private network. This is more reliable and faster than external URLs.

### Internal URL Format

```
http://<service-name>.railway.internal:<port>
```

For Ollama:
```
http://ollama.railway.internal:11434
```

## Configuration

### Option 1: Internal URL (Recommended)

In your **Backend** service environment variables:

```bash
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama.railway.internal:11434
```

**Benefits:**
- ✅ Faster (no external routing)
- ✅ More reliable (no 502 errors)
- ✅ Free (no external bandwidth)
- ✅ Works even if external URL has issues

### Option 2: External URL

If you must use external URL, ensure:
1. Ollama service is listening on Railway's `$PORT`
2. Health checks are passing
3. Service is publicly accessible

```bash
LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app
```

**Note:** The URL will be auto-fixed to include `https://` if missing.

## Finding Your Service Name

1. Go to Railway Dashboard
2. Find your Ollama service
3. The service name is shown at the top (e.g., `ollama`, `ollama-service`)
4. Use that name in the internal URL: `http://<name>.railway.internal:11434`

## Troubleshooting

### Still Getting 502?

1. **Check service name**: Make sure the service name in the internal URL matches exactly
2. **Check port**: Ollama should be on port 11434 (or whatever PORT env var is set)
3. **Check logs**: Verify Ollama is running and listening on the correct port

### URL Validation

The backend automatically:
- Adds `https://` if missing from external URLs
- Adds `http://` if missing from localhost/internal URLs
- Validates URL format before connecting

## Example Configuration

### Backend Environment Variables

```bash
# Use internal URL (recommended)
LANGCHAIN4J_OLLAMA_BASE_URL=http://ollama.railway.internal:11434

# Or use external URL (if needed)
# LANGCHAIN4J_OLLAMA_BASE_URL=https://your-ollama-service.railway.app

LANGCHAIN4J_OLLAMA_MODEL=llama3.1
LANGCHAIN4J_OLLAMA_TEMPERATURE=0.7
```

## Why Internal URLs Work Better

1. **No External Routing**: Traffic stays within Railway's network
2. **No Port Issues**: Direct connection to the service port
3. **No 502 Errors**: Bypasses Railway's external routing layer
4. **Faster**: Lower latency within private network

