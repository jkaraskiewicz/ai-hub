#!/bin/bash

echo "Starting OpenCode AI Hub..."

# Create necessary directories
mkdir -p /app/logs /root/.local/share/opencode /root/.config/opencode

# Set environment variables for API keys (OpenCode will detect these automatically)
export GEMINI_API_KEY="${GEMINI_API_KEY}"
export OPENROUTER_API_KEY="${OPENROUTER_API_KEY}"

echo "Configured API keys:"
echo "- GEMINI_API_KEY: $([ -n "$GEMINI_API_KEY" ] && echo "Set" || echo "Not set")"
echo "- OPENROUTER_API_KEY: $([ -n "$OPENROUTER_API_KEY" ] && echo "Set" || echo "Not set")"

# Show available models
echo "Available models:"
opencode models | head -20

# Container ready
echo "OpenCode AI Hub ready!"
echo "Access Web UI: http://localhost:3001"
echo "For Claude Pro: docker exec -it ai-hub opencode auth login"

# Start OpenCode server in background
echo "Starting OpenCode API server on port 4096..."
opencode serve --port 4096 --hostname 0.0.0.0 &

# Start OpenAI proxy server in background
echo "Starting OpenAI-compatible proxy server on port 8080..."
cd /app && node opencode-proxy.js &

echo "All services started!"
echo "- OpenCode API: http://localhost:4096"
echo "- OpenAI Proxy: http://localhost:8080"
echo "- Ready for Open WebUI integration"

# Keep alive
tail -f /dev/null