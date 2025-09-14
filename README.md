# OpenCode AI Hub with Web UI

A complete containerized AI development environment featuring OpenCode with an integrated web interface.

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