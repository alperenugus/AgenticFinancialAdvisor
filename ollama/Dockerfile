# Dockerfile for Ollama service on Railway
FROM ollama/ollama:latest

# Install curl for health checks and model checking
# Ollama base image is Debian-based, so use apt-get
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Expose Ollama port
# Railway will set $PORT env var - Ollama will listen on that port
# We expose both 11434 (default) and use $PORT dynamically
EXPOSE 11434
# Note: Railway may set PORT to a different value, which is handled in entrypoint.sh

# Set Ollama to listen on all interfaces
# Railway uses $PORT env var, but Ollama needs explicit host:port
# We'll handle this in entrypoint.sh
ENV OLLAMA_KEEP_ALIVE=24h
ENV OLLAMA_MODEL=llama3.1

# Note: Use Railway volumes instead of VOLUME keyword
# Configure volume in Railway dashboard: Settings → Volumes
# Mount path: /root/.ollama

# Copy startup script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Health check (use PORT env var if available, default to 11434)
HEALTHCHECK --interval=30s --timeout=3s --start-period=180s --retries=5 \
  CMD sh -c 'PORT=${PORT:-11434} && curl -f http://localhost:${PORT}/api/tags || exit 1'

# Use entrypoint script to auto-pull model
# Use shell form to ensure proper signal handling
ENTRYPOINT ["/bin/sh", "/entrypoint.sh"]

