#!/bin/sh

# Startup script for Ollama on Railway
# Automatically pulls the model if it doesn't exist

MODEL_NAME="${OLLAMA_MODEL:-llama3.2:1b}"

# Railway uses $PORT env var for external routing
# CRITICAL: Railway routes external traffic to $PORT, so Ollama MUST listen on $PORT
# If PORT is not set, Railway might not route traffic correctly
# Default to 11434 only if PORT is truly not set by Railway
OLLAMA_PORT="${PORT:-11434}"

# Set OLLAMA_HOST to listen on all interfaces with the correct port
# This MUST be set before starting ollama serve
export OLLAMA_HOST="0.0.0.0:${OLLAMA_PORT}"

# Set OLLAMA_KEEP_ALIVE to keep model in memory (24 hours)
# This prevents reloading the model on every request
export OLLAMA_KEEP_ALIVE="${OLLAMA_KEEP_ALIVE:-24h}"

# Log the configuration for debugging
echo "=========================================="
echo "Ollama Configuration:"
echo "  PORT env var: ${PORT:-not set (using default 11434)}"
echo "  OLLAMA_PORT: $OLLAMA_PORT"
echo "  OLLAMA_HOST: $OLLAMA_HOST"
echo "=========================================="

echo "Starting Ollama service..."
echo "Model to ensure: $MODEL_NAME"

# Verify OLLAMA_HOST is set correctly
if [ -z "$OLLAMA_HOST" ]; then
    echo "ERROR: OLLAMA_HOST is not set!"
    exit 1
fi

# Start Ollama in the background
# OLLAMA_HOST must be exported before this
ollama serve &
OLLAMA_PID=$!

# Give Ollama a moment to start
sleep 2

# Wait for Ollama to be ready
echo "Waiting for Ollama to be ready..."
for i in $(seq 1 60); do
    if curl -f http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        echo "Ollama is ready!"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "ERROR: Ollama failed to start after 2 minutes"
        exit 1
    fi
    echo "Waiting... ($i/60)"
    sleep 2
done

# Check if model exists
echo "Checking if model $MODEL_NAME exists..."
if ollama list | grep -q "$MODEL_NAME"; then
    echo "Model $MODEL_NAME already exists. Skipping download."
else
    echo "Model $MODEL_NAME not found. Pulling..."
    ollama pull "$MODEL_NAME"
    if [ $? -eq 0 ]; then
        echo "Successfully pulled $MODEL_NAME"
    else
        echo "Warning: Failed to pull $MODEL_NAME. Service will continue without it."
        echo "Note: If this fails due to disk space, increase Railway volume size or clean up space."
    fi
fi

# List all models
echo "Available models:"
ollama list

# Pre-load the model into memory to avoid reloading on each request
echo "Pre-loading model $MODEL_NAME into memory..."
ollama run "$MODEL_NAME" "Hello" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "Model $MODEL_NAME pre-loaded successfully!"
else
    echo "Warning: Failed to pre-load model, but service will continue."
fi

# Keep the container running
echo "Ollama service is running. Models are ready and loaded in memory."
echo "Ollama PID: $OLLAMA_PID"
echo "Model $MODEL_NAME is kept in memory (OLLAMA_KEEP_ALIVE=24h)"

# Wait for Ollama process to keep container alive
# If Ollama crashes, container will exit (Railway will restart it)
wait $OLLAMA_PID

