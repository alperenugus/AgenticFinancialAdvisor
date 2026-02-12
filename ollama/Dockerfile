# Dockerfile for Ollama service on Railway
FROM ollama/ollama:latest

# Install curl for health checks and model checking
RUN apk add --no-cache curl

# Expose Ollama port
EXPOSE 11434

# Set Ollama to listen on all interfaces
ENV OLLAMA_HOST=0.0.0.0:11434
ENV OLLAMA_KEEP_ALIVE=24h
ENV OLLAMA_MODEL=llama3.1

# Note: Use Railway volumes instead of VOLUME keyword
# Configure volume in Railway dashboard: Settings → Volumes
# Mount path: /root/.ollama

# Copy startup script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=180s --retries=5 \
  CMD curl -f http://localhost:11434/api/tags || exit 1

# Use entrypoint script to auto-pull model
ENTRYPOINT ["/entrypoint.sh"]

