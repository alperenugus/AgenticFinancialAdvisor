# Dockerfile for Ollama service on Railway
FROM ollama/ollama:latest

# Expose Ollama port
EXPOSE 11434

# Set Ollama to listen on all interfaces
ENV OLLAMA_HOST=0.0.0.0:11434
ENV OLLAMA_KEEP_ALIVE=24h

# Create volume for models
VOLUME ["/root/.ollama"]

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:11434/api/tags || exit 1

# Start Ollama server
CMD ["ollama", "serve"]

