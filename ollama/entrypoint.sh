#!/bin/sh

# Startup script for Ollama on Railway
# Automatically pulls the model if it doesn't exist

MODEL_NAME="${OLLAMA_MODEL:-llama3.1}"

# Railway uses $PORT env var, but Ollama needs explicit host:port
# Use Railway's PORT if set, otherwise default to 11434
OLLAMA_PORT="${PORT:-11434}"
export OLLAMA_HOST="0.0.0.0:${OLLAMA_PORT}"

echo "Starting Ollama service..."
echo "Model to ensure: $MODEL_NAME"
echo "Ollama will listen on: $OLLAMA_HOST"

# Start Ollama in the background
ollama serve &
OLLAMA_PID=$!

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

# Keep the container running
echo "Ollama service is running. Models are ready."
echo "Ollama PID: $OLLAMA_PID"

# Wait for Ollama process to keep container alive
# If Ollama crashes, container will exit (Railway will restart it)
wait $OLLAMA_PID

