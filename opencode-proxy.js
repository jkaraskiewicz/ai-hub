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

        // Get actual models from OpenCode CLI
        const { exec } = require('child_process');
        const { promisify } = require('util');
        const execAsync = promisify(exec);

        try {
            // Execute opencode models command to get the real list
            const { stdout } = await execAsync('opencode models');
            const modelLines = stdout.split('\n').filter(line => line.trim() && !line.includes('Error'));

            const models = modelLines.map(modelId => {
                const trimmedId = modelId.trim();
                if (!trimmedId) return null;

                // Extract provider from model ID
                let ownedBy = 'opencode';
                if (trimmedId.includes('openrouter/')) {
                    ownedBy = 'openrouter';
                } else if (trimmedId.includes('anthropic/')) {
                    ownedBy = 'anthropic';
                } else if (trimmedId.includes('google/')) {
                    ownedBy = 'google';
                } else if (trimmedId.includes('openai/')) {
                    ownedBy = 'openai';
                }

                return {
                    id: trimmedId,
                    object: 'model',
                    created: Date.now(),
                    owned_by: ownedBy
                };
            }).filter(model => model !== null);

            console.log(`Found ${models.length} models from OpenCode`);

            res.json({
                object: 'list',
                data: models
            });

        } catch (execError) {
            console.error('Could not execute opencode models command:', execError.message);
            res.status(500).json({
                error: {
                    message: `Failed to fetch models from OpenCode: ${execError.message}`,
                    type: 'opencode_error',
                    code: 'model_fetch_failed'
                }
            });
        }
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
        const sessionResponse = await axios.post(`${OPENCODE_API}/sessions`, {
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

            try {
                // Send message to OpenCode session using correct API endpoint
                const opencodeResponse = await axios.post(`${OPENCODE_API}/sessions/${sessionId}/message`, {
                    content: userMessage,
                    model: model
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
                            content: opencodeResponse.data.content || opencodeResponse.data.message?.content || 'Response from OpenCode'
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
            try {
                // Send message to OpenCode session using correct API endpoint
                const opencodeResponse = await axios.post(`${OPENCODE_API}/sessions/${sessionId}/message`, {
                    content: userMessage,
                    model: model
                });

                const responseContent = opencodeResponse.data.content || opencodeResponse.data.message?.content || 'No response from OpenCode';

                const response = {
                    id: `chatcmpl-${Date.now()}`,
                    object: 'chat.completion',
                    created: Math.floor(Date.now() / 1000),
                    model: model,
                    choices: [{
                        index: 0,
                        message: {
                            role: 'assistant',
                            content: responseContent
                        },
                        finish_reason: 'stop'
                    }],
                    usage: {
                        prompt_tokens: userMessage.length,
                        completion_tokens: responseContent.length,
                        total_tokens: userMessage.length + responseContent.length
                    }
                };

                res.json(response);
            } catch (error) {
                console.error('OpenCode API error in non-streaming:', error.message);
                res.status(500).json({
                    error: {
                        message: `OpenCode API error: ${error.message}`,
                        type: 'opencode_error',
                        code: 'chat_failed'
                    }
                });
            }
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

// OpenAI API compatibility - Legacy completions endpoint
app.post('/v1/completions', async (req, res) => {
    try {
        const { prompt, model, max_tokens, temperature, stream = false } = req.body;

        console.log(`Completions request for model: ${model}`);

        // Convert to chat format for OpenCode
        const messages = [{ role: 'user', content: prompt }];

        // Create a new session in OpenCode
        const sessionResponse = await axios.post(`${OPENCODE_API}/sessions`, {
            title: `WebUI Completions - ${new Date().toISOString()}`
        });

        const sessionId = sessionResponse.data.id;

        if (stream) {
            res.writeHead(200, {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            });

            try {
                const opencodeResponse = await axios.post(`${OPENCODE_API}/sessions/${sessionId}/message`, {
                    content: prompt,
                    model: model
                });

                const responseText = opencodeResponse.data.content || opencodeResponse.data.message?.content || '';

                const completion = {
                    id: `cmpl-${Date.now()}`,
                    object: 'text_completion',
                    created: Math.floor(Date.now() / 1000),
                    model: model,
                    choices: [{
                        text: responseText,
                        index: 0,
                        logprobs: null,
                        finish_reason: 'stop'
                    }]
                };

                res.write(`data: ${JSON.stringify(completion)}\n\n`);
                res.write('data: [DONE]\n\n');
                res.end();

            } catch (error) {
                console.error('OpenCode API error in completions streaming:', error.message);
                res.write(`data: {"error": "OpenCode API error: ${error.message}"}\n\n`);
                res.end();
            }
        } else {
            try {
                const opencodeResponse = await axios.post(`${OPENCODE_API}/sessions/${sessionId}/message`, {
                    content: prompt,
                    model: model
                });

                const responseText = opencodeResponse.data.content || opencodeResponse.data.message?.content || '';

                const response = {
                    id: `cmpl-${Date.now()}`,
                    object: 'text_completion',
                    created: Math.floor(Date.now() / 1000),
                    model: model,
                    choices: [{
                        text: responseText,
                        index: 0,
                        logprobs: null,
                        finish_reason: 'stop'
                    }],
                    usage: {
                        prompt_tokens: prompt.length,
                        completion_tokens: responseText.length,
                        total_tokens: prompt.length + responseText.length
                    }
                };

                res.json(response);
            } catch (error) {
                console.error('OpenCode API error in completions:', error.message);
                res.status(500).json({
                    error: {
                        message: `OpenCode API error: ${error.message}`,
                        type: 'opencode_error',
                        code: 'completion_failed'
                    }
                });
            }
        }
    } catch (error) {
        console.error('Error in completions:', error.message);
        res.status(500).json({
            error: {
                message: `Proxy error: ${error.message}`,
                type: 'proxy_error',
                code: 'internal_error'
            }
        });
    }
});

// OpenAI API compatibility - Embeddings endpoint
app.post('/v1/embeddings', async (req, res) => {
    try {
        const { input, model = 'text-embedding-ada-002' } = req.body;

        console.log(`Embeddings request for model: ${model}`);

        // OpenCode doesn't have built-in embeddings support, so we'll return a placeholder
        // In a real implementation, you'd integrate with an embedding model
        res.status(501).json({
            error: {
                message: 'Embeddings not supported by OpenCode. Consider using a dedicated embedding service.',
                type: 'not_implemented',
                code: 'embeddings_not_supported'
            }
        });

    } catch (error) {
        console.error('Error in embeddings:', error.message);
        res.status(500).json({
            error: {
                message: `Proxy error: ${error.message}`,
                type: 'proxy_error',
                code: 'internal_error'
            }
        });
    }
});

// OpenAI API compatibility - Files endpoint for RAG
app.get('/v1/files', async (req, res) => {
    try {
        console.log('Listing files request');

        // Get file status from OpenCode
        const statusResponse = await axios.get(`${OPENCODE_API}/file/status`);
        const trackedFiles = statusResponse.data || [];

        // Convert OpenCode file format to OpenAI format
        const files = trackedFiles.map((file, index) => ({
            id: `file-${Date.now()}-${index}`,
            object: 'file',
            bytes: file.size || 0,
            created_at: Math.floor(Date.now() / 1000),
            filename: file.path || file.name || `file-${index}`,
            purpose: 'retrieval'
        }));

        res.json({
            object: 'list',
            data: files
        });

    } catch (error) {
        console.error('Error listing files:', error.message);
        res.status(500).json({
            error: {
                message: `Failed to list files: ${error.message}`,
                type: 'opencode_error',
                code: 'file_list_failed'
            }
        });
    }
});

app.get('/v1/files/:fileId', async (req, res) => {
    try {
        const { fileId } = req.params;
        console.log(`Getting file details for: ${fileId}`);

        // For now, return a generic response since OpenCode doesn't have individual file metadata
        res.json({
            id: fileId,
            object: 'file',
            bytes: 0,
            created_at: Math.floor(Date.now() / 1000),
            filename: `file-${fileId}`,
            purpose: 'retrieval'
        });

    } catch (error) {
        console.error('Error getting file:', error.message);
        res.status(500).json({
            error: {
                message: `Failed to get file: ${error.message}`,
                type: 'proxy_error',
                code: 'file_get_failed'
            }
        });
    }
});

app.post('/v1/files', async (req, res) => {
    try {
        console.log('File upload request');

        // OpenCode doesn't have a direct file upload API
        // This would typically handle file uploads for RAG
        res.status(501).json({
            error: {
                message: 'File uploads not supported. Use OpenCode\'s workspace files for RAG operations.',
                type: 'not_implemented',
                code: 'file_upload_not_supported'
            }
        });

    } catch (error) {
        console.error('Error uploading file:', error.message);
        res.status(500).json({
            error: {
                message: `File upload error: ${error.message}`,
                type: 'proxy_error',
                code: 'file_upload_failed'
            }
        });
    }
});

app.delete('/v1/files/:fileId', async (req, res) => {
    try {
        const { fileId } = req.params;
        console.log(`Deleting file: ${fileId}`);

        // OpenCode doesn't have a direct file deletion API
        res.status(501).json({
            error: {
                message: 'File deletion not supported through API. Manage files directly in OpenCode workspace.',
                type: 'not_implemented',
                code: 'file_delete_not_supported'
            }
        });

    } catch (error) {
        console.error('Error deleting file:', error.message);
        res.status(500).json({
            error: {
                message: `File deletion error: ${error.message}`,
                type: 'proxy_error',
                code: 'file_delete_failed'
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
