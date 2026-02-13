#!/bin/sh

# Startup script for Ollama on Railway
# Automatically pulls the model if it doesn't exist

MODEL_NAME="${OLLAMA_MODEL:-llama3.1}"

echo "Starting Ollama service..."
echo "Model to ensure: $MODEL_NAME"

# Start Ollama in the background and capture PID
ollama serve &
OLLAMA_PID=$!

# Wait for Ollama to be ready
echo "Waiting for Ollama to be ready..."
for i in $(seq 1 60); do
    if curl -f http://localhost:11434/api/tags > /dev/null 2>&1; then
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
    fi
fi

# List all models
echo "Available models:"
ollama list

# Keep the container running - monitor Ollama process
echo "Ollama service is running. Models are ready."
echo "Ollama PID: $OLLAMA_PID"

# Keep container alive and monitor Ollama process
while true; do
    # Check if Ollama process is still running
    if ! kill -0 $OLLAMA_PID 2>/dev/null; then
        echo "ERROR: Ollama process died! Restarting..."
        ollama serve &
        OLLAMA_PID=$!
        sleep 5
    fi
    # Health check
    if ! curl -f http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "WARNING: Ollama health check failed, but continuing..."
    fi
    sleep 30
done

