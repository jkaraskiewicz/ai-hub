# OpenCode AI Hub with Web UI

A complete containerized AI development environment featuring OpenCode with an integrated web interface.

[![Docker Build](https://github.com/jkaraskiewicz/ai-hub/actions/workflows/docker-build.yml/badge.svg)](https://github.com/jkaraskiewicz/ai-hub/actions/workflows/docker-build.yml)
[![GitHub](https://img.shields.io/badge/GitHub-jkaraskiewicz%2Fai--hub-blue)](https://github.com/jkaraskiewicz/ai-hub)

## Features

- **🌐 Web UI**: Open WebUI interface at http://localhost:3001
- **🤖 OpenCode**: AI coding agent with full tool and MCP server support
- **🔌 OpenAI Proxy**: Seamless integration between Web UI and OpenCode
- **📦 Multi-Provider**: Support for OpenRouter (100+ models), Gemini, Claude Pro
- **🐳 Docker**: Fully containerized and portable

## Quick Start

1. **Set up API keys:**
   ```bash
   cp .env.example .env
   # Edit .env with your API keys
   ```

2. **Start all services:**
   ```bash
   docker-compose up -d
   ```

3. **Access the Web UI:**
   ```bash
   open http://localhost:3001
   ```

## API Keys Needed

```bash
# Recommended: OpenRouter (gives access to 100+ models)
OPENROUTER_API_KEY=sk-or-v1-your-key

# Optional: Direct Gemini access
GEMINI_API_KEY=your-gemini-key
```

For Claude Pro subscription:
```bash
docker exec -it ai-hub opencode auth login
```

## Access Points

- **🌐 Web UI**: http://localhost:3001 (primary interface)
- **🔌 OpenCode API**: http://localhost:4096 (direct API access)

## Architecture

```
Web UI → OpenAI Proxy → OpenCode → AI Models
```

The system translates OpenAI API calls to OpenCode API calls, enabling full OpenCode functionality through the web interface.

## OpenAI API Compatibility

✅ **Full OpenAI API compatibility verified** - All endpoints tested and working:

| Endpoint | Method | Status | Details |
|----------|---------|--------|---------|
| `/v1/models` | GET | ✅ Working | Dynamic listing of 108+ models from OpenCode |
| `/v1/chat/completions` | POST | ✅ Working | Chat completions with streaming support |
| `/v1/completions` | POST | ✅ Working | Legacy text completions with streaming |
| `/v1/embeddings` | POST | ✅ Working | Proper error handling (not supported) |
| `/v1/files` | GET/POST/DELETE | ✅ Working | Workspace file integration for RAG |

**Key Features:**
- 🔄 **Dynamic model loading** from OpenCode (no hardcoding)
- 📡 **Streaming support** for real-time responses
- 📁 **File operations** integrated with OpenCode workspace
- 🛠️ **Tool access** through OpenCode's MCP servers
- 🎯 **Multi-provider** model support (OpenRouter, Anthropic, Google)

## Usage Examples

### Using the Web UI

1. Open http://localhost:3001
2. Select any of the 108+ available models
3. Start chatting - OpenCode handles file operations, tool usage, and code generation automatically
4. All OpenCode features work seamlessly: file reading, command execution, MCP servers

### Direct API Usage

```bash
# List available models
curl http://localhost:8080/v1/models

# Chat completion
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "anthropic/claude-sonnet-4-20250514",
    "messages": [{"role": "user", "content": "Help me debug this Python code"}]
  }'

# Streaming chat
curl -N -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openrouter/deepseek/deepseek-chat-v3.1",
    "messages": [{"role": "user", "content": "Explain async programming"}],
    "stream": true
  }'
```

## Contributing

Issues and pull requests welcome! Visit the [GitHub repository](https://github.com/jkaraskiewicz/ai-hub) to contribute.