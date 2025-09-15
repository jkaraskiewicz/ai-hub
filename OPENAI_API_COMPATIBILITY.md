# OpenAI API Compatibility Guide

## What is "OpenAI API Compatible"?

OpenAI API compatibility means implementing the same REST API endpoints and request/response formats that OpenAI uses. This allows any application built for OpenAI (like Open WebUI, LangChain, etc.) to work with your service without modifications - they just point to your proxy URL instead of OpenAI's API.

## Core OpenAI API Endpoints

### 1. **GET /v1/models**
- **Purpose**: Lists all available models
- **Response**: Array of model objects with `id`, `object`, `created`, `owned_by`
- **Usage**: Model selection in chat interfaces

### 2. **POST /v1/chat/completions**
- **Purpose**: Chat-based completions (GPT-3.5/4 style conversations)
- **Features**:
  - Streaming support (`"stream": true`)
  - Message history (`messages` array)
  - System/user/assistant roles
- **Response**: Chat completion with `choices`, `usage`, etc.
- **Usage**: Modern conversational AI interfaces

### 3. **POST /v1/completions**
- **Purpose**: Legacy text completions (GPT-3 style)
- **Features**:
  - Single prompt input
  - Streaming support
  - Text continuation
- **Response**: Text completion with `choices[].text`
- **Usage**: Older completion-based applications

### 4. **POST /v1/embeddings**
- **Purpose**: Generate text embeddings for semantic search
- **Input**: Text strings to embed
- **Response**: Vector embeddings
- **Usage**: RAG systems, semantic search, clustering

### 5. **File Operations** (for RAG)
- **GET /v1/files**: List uploaded files
- **GET /v1/files/:fileId**: Get file metadata
- **POST /v1/files**: Upload files for RAG
- **DELETE /v1/files/:fileId**: Delete files
- **Usage**: Document-based Q&A, knowledge bases

## Key OpenAI API Features

### **Request/Response Format**
- **Content-Type**: `application/json`
- **Authentication**: `Authorization: Bearer sk-...` header
- **Error Format**: Structured error objects with `message`, `type`, `code`

### **Streaming Support**
- **Format**: Server-Sent Events (SSE)
- **Content-Type**: `text/event-stream`
- **Structure**: `data: {json}\n\n` chunks
- **Termination**: `data: [DONE]\n\n`

### **Chat Messages Format**
```json
{
  "model": "gpt-4",
  "messages": [
    {"role": "system", "content": "You are a helpful assistant"},
    {"role": "user", "content": "Hello"},
    {"role": "assistant", "content": "Hi! How can I help?"},
    {"role": "user", "content": "What's the weather?"}
  ],
  "stream": false
}
```

### **Response Format**
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-4",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "The weather is..."
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 20,
    "total_tokens": 30
  }
}
```

## Benefits of OpenAI API Compatibility

### **Ecosystem Integration**
- ✅ **Drop-in replacement** for OpenAI
- ✅ **Works with existing tools**: LangChain, Open WebUI, etc.
- ✅ **No client modifications** needed
- ✅ **Standard tooling** support

### **Developer Experience**
- 🔄 **Familiar API patterns**
- 📚 **Extensive documentation** available
- 🛠️ **Rich tooling ecosystem**
- 🔌 **Easy migration** between providers

## Implementation Notes

### **Authentication**
- Most proxies use dummy keys or bypass auth
- Original OpenAI uses `sk-...` API keys
- Some providers use their own key formats

### **Model Naming**
- OpenAI: `gpt-4`, `gpt-3.5-turbo`
- Other providers: `provider/model-name` format
- Example: `anthropic/claude-sonnet-4`, `openrouter/deepseek/deepseek-chat`

### **Error Handling**
- Proper HTTP status codes (400, 401, 429, 500)
- Structured error responses matching OpenAI format
- Helpful error messages for debugging

### **Rate Limiting**
- Standard HTTP 429 responses
- `Retry-After` headers
- Rate limit info in headers

## Common Applications Using OpenAI API

- **Chat Interfaces**: Open WebUI, ChatBot UI
- **Development Tools**: GitHub Copilot, Cursor
- **AI Frameworks**: LangChain, LlamaIndex
- **Automation**: Zapier, Make.com
- **Custom Applications**: Any app using OpenAI client libraries

## Testing API Compatibility

Essential tests for full compatibility:
1. ✅ Model listing works
2. ✅ Chat completions (streaming & non-streaming)
3. ✅ Text completions (streaming & non-streaming)
4. ✅ Proper error responses
5. ✅ File operations (if supported)
6. ✅ Authentication handling
7. ✅ Rate limiting responses

---

*This compatibility allows seamless integration with the broader AI ecosystem without vendor lock-in.*