#!/usr/bin/env node

const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
const PORT = 8080;
const OPENCODE_API = 'http://localhost:4096';

app.use(cors());
app.use(express.json());

// Middleware for logging
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// OpenAI API compatibility - Models endpoint
app.get('/v1/models', async (req, res) => {
    try {
        console.log('Fetching models from OpenCode...');

        // Get available models from OpenCode
        const response = await axios.get(`${OPENCODE_API}/config`);

        // For now, we'll create a static list based on known OpenCode models
        // In a real implementation, you'd parse OpenCode's model list
        const models = [
            {
                id: 'openrouter/deepseek/deepseek-chat-v3.1',
                object: 'model',
                created: Date.now(),
                owned_by: 'openrouter'
            },
            {
                id: 'openrouter/anthropic/claude-sonnet-4',
                object: 'model',
                created: Date.now(),
                owned_by: 'openrouter'
            },
            {
                id: 'openrouter/google/gemini-2.0-flash-exp:free',
                object: 'model',
                created: Date.now(),
                owned_by: 'openrouter'
            },
            {
                id: 'openrouter/deepseek/deepseek-r1:free',
                object: 'model',
                created: Date.now(),
                owned_by: 'openrouter'
            }
        ];

        res.json({
            object: 'list',
            data: models
        });
    } catch (error) {
        console.error('Error fetching models:', error.message);
        res.status(500).json({ error: 'Failed to fetch models' });
    }
});

// OpenAI API compatibility - Chat completions endpoint
app.post('/v1/chat/completions', async (req, res) => {
    try {
        const { messages, model, stream = false } = req.body;

        console.log(`Chat completion request for model: ${model}`);
        console.log(`Messages: ${JSON.stringify(messages, null, 2)}`);

        // Extract the user message (last message in the array)
        const userMessage = messages[messages.length - 1]?.content || '';

        // Create a new session in OpenCode
        const sessionResponse = await axios.post(`${OPENCODE_API}/session`, {
            title: `WebUI Chat - ${new Date().toISOString()}`
        });

        const sessionId = sessionResponse.data.id;
        console.log(`Created OpenCode session: ${sessionId}`);

        if (stream) {
            // Handle streaming response
            res.writeHead(200, {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            });

            // Start a conversation in OpenCode with the selected model
            const chatData = {
                sessionId: sessionId,
                message: userMessage,
                model: model
            };

            try {
                // Use OpenCode's TUI endpoint to send message
                const opencodeResponse = await axios.post(`${OPENCODE_API}/tui`, {
                    type: 'chat',
                    data: chatData
                });

                // For simplicity, return a non-streaming response formatted as streaming
                const completion = {
                    id: `chatcmpl-${Date.now()}`,
                    object: 'chat.completion.chunk',
                    created: Math.floor(Date.now() / 1000),
                    model: model,
                    choices: [{
                        index: 0,
                        delta: {
                            content: opencodeResponse.data.response || 'Response from OpenCode'
                        },
                        finish_reason: null
                    }]
                };

                res.write(`data: ${JSON.stringify(completion)}\n\n`);

                // Send final chunk
                const finalChunk = {
                    ...completion,
                    choices: [{
                        index: 0,
                        delta: {},
                        finish_reason: 'stop'
                    }]
                };

                res.write(`data: ${JSON.stringify(finalChunk)}\n\n`);
                res.write('data: [DONE]\n\n');
                res.end();

            } catch (error) {
                console.error('OpenCode API error:', error.message);
                res.write(`data: {"error": "OpenCode API error: ${error.message}"}\n\n`);
                res.end();
            }
        } else {
            // Handle non-streaming response
            // For now, let's create a simple response
            // In a real implementation, you'd call OpenCode's API properly

            const response = {
                id: `chatcmpl-${Date.now()}`,
                object: 'chat.completion',
                created: Math.floor(Date.now() / 1000),
                model: model,
                choices: [{
                    index: 0,
                    message: {
                        role: 'assistant',
                        content: `Hello! I'm OpenCode responding via the proxy. You asked: "${userMessage}". I'm using model: ${model}`
                    },
                    finish_reason: 'stop'
                }],
                usage: {
                    prompt_tokens: userMessage.length,
                    completion_tokens: 50,
                    total_tokens: userMessage.length + 50
                }
            };

            res.json(response);
        }
    } catch (error) {
        console.error('Error in chat completion:', error.message);
        res.status(500).json({ 
            error: {
                message: `Proxy error: ${error.message}`,
                type: 'proxy_error',
                code: 'internal_error'
            }
        });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`OpenCode-OpenAI Proxy Server running on http://0.0.0.0:${PORT}`);
    console.log(`Connecting to OpenCode API at: ${OPENCODE_API}`);
});
