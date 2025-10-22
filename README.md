# LibreLog - GayPHX Radio Traffic System

A professional radio traffic, scheduling, and automation system built to integrate LibreTime and AzuraCast for GayPHX Radio.

## Architecture

- **Backend**: FastAPI + Python 3.11+ with PostgreSQL
- **Frontend**: Vite + React + TypeScript with Material-UI
- **Deployment**: Docker Compose on same server as LibreTime
- **Integrations**: LibreTime API, AzuraCast API, LibreTalk (future)

## Quick Start

1. **Copy environment template:**
   ```bash
   cp .cursor/env.template .env
   ```

2. **Configure API keys in `.env`:**
   ```bash
   # Edit .env with your LibreTime and AzuraCast API keys
   LIBRETIME_URL=https://your-libretime-url/api
   LIBRETIME_API_KEY=your-libretime-api-key
   AZURACAST_URL=https://your-azuracast-url/api
   AZURACAST_API_KEY=your-azuracast-api-key
   ```

3. **Start the system:**
   ```bash
   docker-compose up -d
   ```

4. **Access the application:**
   - **Frontend**: http://localhost:3000
   - **API**: http://localhost:8000
   - **API Docs**: http://localhost:8000/docs
   - **Setup Page**: http://localhost:3000/setup

5. **Initial Setup:**
   - Go to http://localhost:3000/setup
   - Create your admin user
   - Verify API key configuration
   - Login at http://localhost:3000/login

## Default Login Credentials

After running initial setup:
- **Username**: `admin`
- **Password**: `admin123`

**⚠️ Change this password immediately after first login!**

## Development

See `docs/DEVELOPMENT.md` for detailed setup instructions.

## Features (Alpha)

- ✅ Music library management with metadata tagging
- ✅ Clock template builder for radio scheduling
- ✅ Campaign and PSA management with ad fallback
- ✅ Daily log generation and LibreTime integration
- ✅ Voice tracking with web recorder
- ✅ Playback reconciliation and compliance reporting
- ✅ AzuraCast metadata sync

## License

Proprietary - GayPHX Radio
