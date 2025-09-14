# OpenCode AI Hub - Complete Usage Guide

## 🚀 Quick Start

### Starting the Services
```bash
# Start all services (OpenCode + Web UI)
docker-compose up -d
```

This starts:
- **OpenCode API Server** (port 4096)
- **OpenAI-compatible Proxy** (port 8080) 
- **Web UI** (http://localhost:3001)

### Access Methods

#### 🌐 Web UI (Recommended)
```bash
# Open in your browser
open http://localhost:3001
```
- Full chat interface like ChatGPT/Claude
- Model selection dropdown
- Chat history and sessions
- File uploads and attachments

#### 💻 Command Line
```bash
# Simple conversation
docker exec ai-hub opencode run "Hello! What can you help me with?"

# Interactive mode
docker exec -it ai-hub opencode

# Use specific model
docker exec ai-hub opencode --model "openrouter/deepseek/deepseek-chat-v3.1" run "Your prompt"
```

#### 🔌 API Access
```bash
# OpenAI-compatible API (for integrations)
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "openrouter/deepseek/deepseek-chat-v3.1", "messages": [{"role": "user", "content": "Hello!"}]}'
```

## 🎯 Popular Models

### Free Models
```bash
# DeepSeek Chat (high quality, affordable)
docker exec ai-hub opencode --model "openrouter/deepseek/deepseek-chat-v3.1" run "Your prompt"

# Free Gemini (fast responses)
docker exec ai-hub opencode --model "openrouter/google/gemini-2.0-flash-exp:free" run "Your prompt"
```

### Premium Models
```bash
# Claude Sonnet 4 (best for coding)
docker exec ai-hub opencode --model "openrouter/anthropic/claude-sonnet-4" run "Your prompt"

# Gemini 2.5 Pro (long context)
docker exec ai-hub opencode --model "openrouter/google/gemini-2.5-pro" run "Your prompt"
```

### Claude Pro (Interactive Authentication Required)
```bash
# Login to Claude Pro (requires interactive terminal)
docker exec -it ai-hub opencode auth login

# Check authentication status
docker exec ai-hub opencode auth list

# Use Claude Pro (after authentication)
docker exec ai-hub opencode run "Your prompt"  # Uses authenticated Claude
```

### Direct Google Gemini (with GEMINI_API_KEY)
```bash
# Test if Gemini API key is configured
docker exec -e GEMINI_API_KEY="your_api_key" ai-hub opencode run --model "google/gemini-2.5-flash" "Hello"
```

## 🔧 Configuration

### Environment Variables (.env file)
```bash
# Copy template
cp .env.example .env

# Configure your API keys
OPENROUTER_API_KEY=your_openrouter_key        # Recommended - gives access to many models
GEMINI_API_KEY=your_google_api_key            # Optional - direct Gemini access
# Note: Claude Pro uses subscription auth, not API key
```

### Authentication Status
```bash
# Check what providers are available
docker exec ai-hub opencode auth list

# Expected output shows OpenRouter environment variable detected
# If Claude Pro authenticated, it will show credentials
```

## 📊 Model Recommendations

| Use Case | Recommended Model | Command |
|----------|-------------------|---------|
| **Free Usage** | DeepSeek R1 | `--model "openrouter/deepseek/deepseek-r1:free"` |
| **Code Generation** | Claude Sonnet 4 | `--model "openrouter/anthropic/claude-sonnet-4"` |
| **Long Context** | Gemini 2.5 Pro | `--model "openrouter/google/gemini-2.5-pro"` |
| **Fast & Cheap** | Gemini Flash | `--model "openrouter/google/gemini-2.0-flash-exp:free"` |
| **Reasoning** | DeepSeek Chat | `--model "openrouter/deepseek/deepseek-chat-v3.1"` |

## 🚀 Usage Examples

### Code Development
```bash
# Generate Python function
docker exec ai-hub opencode run --model "openrouter/deepseek/deepseek-chat-v3.1" \
  "Write a Python function to calculate fibonacci numbers with memoization"

# Code review
docker exec ai-hub opencode run --model "openrouter/anthropic/claude-sonnet-4" \
  "Review this code for security issues: [paste your code]"
```

### Analysis & Research
```bash
# Document analysis
docker exec ai-hub opencode run --model "openrouter/google/gemini-2.5-pro" \
  "Analyze and summarize this technical document: [paste content]"

# Quick questions
docker exec ai-hub opencode run --model "openrouter/google/gemini-2.0-flash-exp:free" \
  "What is the difference between REST and GraphQL?"
```

### Interactive Sessions
```bash
# Start interactive mode (requires TTY)
docker exec -it ai-hub opencode

# Within interactive mode, you can:
# - Have multi-turn conversations
# - Switch between models
# - Edit files directly
```

## 🌐 Web UI Features

The integrated Web UI (Open WebUI) provides:

### ✨ Full Chat Experience
- **Model Selection**: Choose from all available OpenCode models
- **Chat History**: Persistent conversations and sessions  
- **Real-time Streaming**: Live response generation
- **Tool Usage**: OpenCode's tools and MCP servers work through the Web UI
- **File Uploads**: Send files and images to models that support them

### 🎯 How It Works
1. **Web UI** → sends OpenAI API requests
2. **Proxy Server** → translates to OpenCode API calls  
3. **OpenCode** → processes with full tool/MCP capabilities
4. **Response** → streams back through the chain

### 🔧 Configuration
- Access: http://localhost:3001
- No authentication required (local only)
- Models auto-detected from OpenCode
- Supports all OpenCode features including tools and file operations

## 🔐 Authentication Setup

### OpenRouter (Recommended)
1. Get API key from https://openrouter.ai/
2. Add to `.env` file: `OPENROUTER_API_KEY=your_key`
3. Restart services: `docker-compose restart`

### Claude Pro
1. Must have active Claude Pro subscription
2. Run: `docker exec -it ai-hub opencode auth login`
3. Follow browser authentication flow
4. Authentication persists in mounted volume

### Direct Provider APIs
```bash
# Gemini API
GEMINI_API_KEY=your_google_api_key

# Note: Claude Pro uses subscription auth instead of API key
```

## 🚨 Troubleshooting

### Common Issues

**"No endpoints found that support tool use"**
- Some models don't support OpenCode's advanced features
- Use simpler queries or try different models

**"ProviderModelNotFoundError"**
- Check if API key is set correctly
- Verify model name with `opencode models`

**Authentication failures**
- For Claude Pro: Re-run `opencode auth login`
- For API keys: Check `.env` file and restart container

### Debug Commands
```bash
# Check available models
docker exec ai-hub opencode models | head -20

# Check authentication status
docker exec ai-hub opencode auth list

# View recent logs
docker exec ai-hub cat /root/.local/share/opencode/log/*.log | tail -50

# Check environment variables
docker exec ai-hub env | grep -E "(GEMINI|OPENROUTER)_API_KEY"
```

## 💡 Tips

1. **Free Usage**: Start with `openrouter/deepseek/deepseek-r1:free` - it's free and powerful
2. **Performance**: OpenRouter models via API key are faster than Claude Pro auth
3. **Persistence**: Claude Pro auth and logs are persisted in mounted volumes
4. **Model Selection**: Use `opencode models` to see all available options
5. **Context**: Some models have larger context windows - check provider docs