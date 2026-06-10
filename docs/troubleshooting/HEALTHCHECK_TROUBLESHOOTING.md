# Health Check Troubleshooting

> This page previously documented an Ollama-based setup that the project no longer uses. The current
> stack is **OpenAI** (LLM) + **Finnhub/Yahoo Finance** (market data) — there is no Ollama service.

The canonical, up-to-date guide for Railway health-check failures lives here:

➡️ **[RAILWAY_HEALTHCHECK_FIX.md](./RAILWAY_HEALTHCHECK_FIX.md)**

## TL;DR

The Railway health-check path **must be public**. If it points at an authentication-protected route, Spring
Security answers with a **302 redirect to Google login**, Railway reads that as "service unavailable," and the
deploy is marked **FAILED** while a stale build keeps serving.

The fix (in place): `GET /api/health` and `/api/advisor/status` are `permitAll` in `SecurityConfig`, and
`railway.toml` / the Docker `HEALTHCHECK` point at `/api/health`. Also ensure a strong **`JWT_SECRET`** is set —
without it the app fails fast at startup and the health check (correctly) fails because nothing is listening.

```bash
# Healthy response is HTTP 200 with {"status":"UP",...}, NOT a 302
curl -i https://<your-backend>.up.railway.app/api/health
```
