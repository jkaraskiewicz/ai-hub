#!/usr/bin/env node

const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
const PORT = 8080;
const OPENCODE_API = 'http://localhost:4096';

// Session management: Map conversation IDs to OpenCode session IDs
const conversationSessions = new Map();

// Helper function to create a conversation ID from messages
function getConversationId(messages) {
    // Create a stable ID based on the first user message content
    // This allows us to identify the same conversation across requests
    if (messages && messages.length > 0) {
        const firstUserMessage = messages.find(m => m.role === 'user')?.content || '';
        const hash = Buffer.from(firstUserMessage).toString('base64').slice(0, 16);
        return `conv_${hash}`;
    }
    return `conv_${Date.now()}`;
}

// Helper function to get or create OpenCode session for a conversation
async function getOrCreateSession(conversationId, messages) {
    if (conversationSessions.has(conversationId)) {
        const sessionId = conversationSessions.get(conversationId);
        console.log(`Reusing existing session: ${sessionId} for conversation: ${conversationId}`);
        return sessionId;
    }

    // Create new OpenCode session
    const sessionResponse = await axios.post(`${OPENCODE_API}/session`, {
        title: `WebUI Chat - ${new Date().toISOString()}`
    });

    const sessionId = sessionResponse.data.id;
    conversationSessions.set(conversationId, sessionId);
    console.log(`Created new session: ${sessionId} for conversation: ${conversationId}`);

    return sessionId;
}

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
        console.log(`Starting chat completion handler`);
        const { messages, model, stream = false } = req.body;

        console.log(`Chat completion request for model: ${model}`);
        console.log(`Messages: ${JSON.stringify(messages, null, 2)}`);
        console.log(`OpenCode API URL: ${OPENCODE_API}`);

        // Get or create session for this conversation
        const conversationId = getConversationId(messages);
        console.log(`Generated conversation ID: ${conversationId} for ${messages.length} messages`);
        const sessionId = await getOrCreateSession(conversationId, messages);

        // Extract the user message (last message in the array)
        const userMessage = messages[messages.length - 1]?.content || '';
        console.log(`Extracted user message: ${userMessage}`);

        if (stream) {
            // Handle streaming response
            res.writeHead(200, {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            });

            try {
                // Send message to OpenCode session using correct API endpoint
                const requestData = {
                    parts: [{
                        type: 'text',
                        text: userMessage
                    }],
                    providerID: model.includes('/') ? model.split('/')[0] : 'openrouter',
                    modelID: model
                };
                console.log(`Sending request to ${OPENCODE_API}/session/${sessionId}/message with data:`, JSON.stringify(requestData, null, 2));
                const opencodeResponse = await axios.post(`${OPENCODE_API}/session/${sessionId}/message`, requestData);

                // Extract text from the response parts
                const textParts = opencodeResponse.data.parts?.filter(part => part.type === 'text') || [];
                const responseText = textParts.map(part => part.text).join('') || 'No response from OpenCode';

                // For simplicity, return a non-streaming response formatted as streaming
                const completion = {
                    id: `chatcmpl-${Date.now()}`,
                    object: 'chat.completion.chunk',
                    created: Math.floor(Date.now() / 1000),
                    model: model,
                    choices: [{
                        index: 0,
                        delta: {
                            content: responseText
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
                const requestData = {
                    parts: [{
                        type: 'text',
                        text: userMessage
                    }],
                    providerID: model.includes('/') ? model.split('/')[0] : 'openrouter',
                    modelID: model
                };
                console.log(`Non-streaming request to ${OPENCODE_API}/session/${sessionId}/message with data:`, JSON.stringify(requestData, null, 2));
                const opencodeResponse = await axios.post(`${OPENCODE_API}/session/${sessionId}/message`, requestData);

                // Extract text from the response parts
                const textParts = opencodeResponse.data.parts?.filter(part => part.type === 'text') || [];
                const responseContent = textParts.map(part => part.text).join('') || 'No response from OpenCode';

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
                console.error('Error response:', error.response?.data);
                console.error('Error status:', error.response?.status);
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

        // For completions, create a simple conversation ID based on prompt
        const fakeMessages = [{ role: 'user', content: prompt }];
        const conversationId = getConversationId(fakeMessages);
        const sessionId = await getOrCreateSession(conversationId, fakeMessages);

        if (stream) {
            res.writeHead(200, {
                'Content-Type': 'text/event-stream',
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive'
            });

            try {
                // Send prompt to OpenCode session using correct API endpoint
                const opencodeResponse = await axios.post(`${OPENCODE_API}/session/${sessionId}/message`, {
                    parts: [{
                        type: 'text',
                        text: prompt
                    }],
                    providerID: model.includes('/') ? model.split('/')[0] : 'openrouter',
                    modelID: model
                });

                // Extract text from the response parts
                const textParts = opencodeResponse.data.parts?.filter(part => part.type === 'text') || [];
                const responseText = textParts.map(part => part.text).join('') || 'No response from OpenCode';

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
                // Send prompt to OpenCode session using correct API endpoint
                const opencodeResponse = await axios.post(`${OPENCODE_API}/session/${sessionId}/message`, {
                    parts: [{
                        type: 'text',
                        text: prompt
                    }],
                    providerID: model.includes('/') ? model.split('/')[0] : 'openrouter',
                    modelID: model
                });

                // Extract text from the response parts
                const textParts = opencodeResponse.data.parts?.filter(part => part.type === 'text') || [];
                const responseText = textParts.map(part => part.text).join('') || 'No response from OpenCode';

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

        // Get files from OpenCode's find/file endpoint
        let trackedFiles = [];
        try {
            // Get all files in the workspace
            const filesResponse = await axios.get(`${OPENCODE_API}/find/file?query=`);
            const allFiles = filesResponse.data || [];

            // Filter out node_modules and other unwanted files
            const workspaceFiles = allFiles.filter(filePath =>
                !filePath.includes('node_modules/') &&
                !filePath.includes('.git/') &&
                !filePath.startsWith('.')
            );

            trackedFiles = workspaceFiles.map(filePath => ({
                path: filePath,
                name: filePath.split('/').pop(),
                // We don't have size info from this endpoint
                size: 0
            }));

            console.log(`Found ${trackedFiles.length} workspace files`);
        } catch (fileError) {
            console.warn('Could not fetch files from OpenCode:', fileError.message);
            trackedFiles = [];
        }

        // Convert OpenCode file format to OpenAI format
        const files = trackedFiles.map((file) => ({
            id: `file-${Buffer.from(file.path).toString('base64')}`,
            object: 'file',
            bytes: file.size || 0,
            created_at: Math.floor(Date.now() / 1000),
            filename: file.name || file.path,
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

        // Extract filename from fileId (format: file-<base64-encoded-path>)
        if (fileId.startsWith('file-')) {
            try {
                const encodedPath = fileId.substring(5); // Remove 'file-' prefix
                const filePath = Buffer.from(encodedPath, 'base64').toString('utf-8');

                console.log(`Decoded file path: ${filePath}`);

                res.json({
                    id: fileId,
                    object: 'file',
                    bytes: 0, // We don't have size info readily available
                    created_at: Math.floor(Date.now() / 1000),
                    filename: filePath.split('/').pop(),
                    purpose: 'retrieval'
                });
            } catch (decodeError) {
                console.error('Error decoding file ID:', decodeError.message);
                res.json({
                    id: fileId,
                    object: 'file',
                    bytes: 0,
                    created_at: Math.floor(Date.now() / 1000),
                    filename: `file-${fileId}`,
                    purpose: 'retrieval'
                });
            }
        } else {
            res.json({
                id: fileId,
                object: 'file',
                bytes: 0,
                created_at: Math.floor(Date.now() / 1000),
                filename: `file-${fileId}`,
                purpose: 'retrieval'
            });
        }

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
