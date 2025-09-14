FROM node:20-bullseye-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create working directory
WORKDIR /app

# Install OpenCode globally
RUN npm install -g opencode-ai@latest

# Copy and install proxy dependencies
COPY package.json /app/package.json
COPY opencode-proxy.js /app/opencode-proxy.js
RUN cd /app && npm install

# Create directories for OpenCode configuration and auth
RUN mkdir -p /root/.local/share/opencode /root/.config/opencode

# Copy startup script
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

# Expose ports
EXPOSE 4096 8080

# Environment variables for API keys
ENV GEMINI_API_KEY=""
ENV OPENROUTER_API_KEY=""

CMD ["/app/startup.sh"]