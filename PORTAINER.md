# Portainer Deployment Guide

## 🚀 Using Pre-built GitHub Container Registry Images

After each push to your repository, GitHub Actions automatically builds and pushes Docker images to GitHub Container Registry (GHCR).

### 📦 Image Location
```
ghcr.io/your-username/your-repo-name:latest
```

### 🔧 Portainer Setup Steps

#### 1. **Access Portainer**
- Navigate to https://localhost:9443
- Login with your admin credentials

#### 2. **Create New Stack**
- **Stacks** → **Add Stack**
- **Name**: `opencode-ai-hub`
- **Build method**: Web editor

#### 3. **Use This Docker Compose**
```yaml
services:
  ai-hub:
    image: ghcr.io/YOUR_USERNAME/YOUR_REPO_NAME:latest
    container_name: ai-hub
    environment:
      - GEMINI_API_KEY=${GEMINI_API_KEY}
      - OPENROUTER_API_KEY=${OPENROUTER_API_KEY}
    ports:
      - "4096:4096"  # OpenCode API server
    volumes:
      - ./workdir:/app/workdir
      - ./logs:/app/logs
      - ./opencode-auth:/root/.local/share/opencode
    stdin_open: true
    tty: true
    restart: unless-stopped
    networks:
      - ai-hub-network

  opencode-webui:
    image: ghcr.io/open-webui/open-webui:main
    container_name: opencode-webui
    ports:
      - "3001:8080"
    environment:
      - OPENAI_API_BASE_URL=http://ai-hub:8080/v1
      - OPENAI_API_KEY=dummy-key
    volumes:
      - ./opencode-webui-data:/app/backend/data
    depends_on:
      - ai-hub
    restart: unless-stopped
    networks:
      - ai-hub-network

networks:
  ai-hub-network:
    driver: bridge
```

#### 4. **Set Environment Variables**
In Portainer's environment variables section:
```env
OPENROUTER_API_KEY=sk-or-v1-your-actual-key-here
GEMINI_API_KEY=your-gemini-key-here
```

#### 5. **Deploy Stack**
- Click **Deploy the stack**
- Portainer will pull the pre-built images and start the services

### 🔐 **GitHub Container Registry Authentication**

If your repository is private, you'll need to authenticate:

1. **Create Personal Access Token**
   - GitHub → Settings → Developer settings → Personal access tokens
   - Create token with `read:packages` scope

2. **Add Registry in Portainer**
   - **Registries** → **Add Registry**
   - **Type**: Custom Registry
   - **Name**: GitHub Container Registry
   - **Registry URL**: `ghcr.io`
   - **Username**: Your GitHub username
   - **Password**: Your personal access token

### 🔄 **Updating to Latest Build**

To update to the latest image:
1. Go to your stack in Portainer
2. Click **Editor**
3. Click **Update the stack**
4. Check **Re-pull image and redeploy**
5. **Update**

Or use Portainer webhooks for automatic updates on new builds.

### 📊 **Available Image Tags**

GitHub Actions creates these tags:
- `latest` - Latest main/master branch
- `main` or `master` - Branch-specific
- `v1.0.0` - Version tags (if you create releases)
- `pr-123` - Pull request builds

### 💡 **Benefits of This Approach**

✅ **Automated Builds** - No manual Docker building
✅ **Multi-Architecture** - Supports AMD64 and ARM64
✅ **Versioned Images** - Tagged releases for rollbacks
✅ **CI/CD Integration** - Builds on every commit
✅ **Portainer Ready** - Pre-configured compose files