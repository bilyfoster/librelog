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

## Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[User Manual](docs/USER_MANUAL.md)** - Complete user guide
- **[Workflow Scenarios](docs/WORKFLOW_SCENARIOS.md)** - End-to-end workflow examples
- **[Development Guide](docs/DEVELOPMENT.md)** - Developer setup and guidelines
- **[Security & QA Reports](docs/security/)** - Security audit and QA test reports
- **[Testing Documentation](docs/testing/)** - API testing guides and checklists
- **[Setup Guides](docs/setup/)** - Integration and infrastructure setup
- **[Implementation Status](docs/implementation/)** - Implementation and fix documentation

See [docs/README.md](docs/README.md) for complete documentation index.

## Testing

### API Testing

Comprehensive API testing scripts are available:

- **`test_all_endpoints.py`** - Tests all API endpoints with tokenized requests
- **`test_complete_workflow.py`** - Tests complete workflow from order to billing

**Quick Test:**
```bash
export LIBRELOG_API_URL=http://api:8000
export TEST_USERNAME=admin
export TEST_PASSWORD=admin123
python3 test_all_endpoints.py
```

### Testing Documentation

See the [testing documentation](docs/testing/) directory for:
- [API Testing Guide](docs/testing/API_TESTING_GUIDE.md) - Complete guide to API testing
- [Manual Testing Checklist](docs/testing/MANUAL_TESTING_CHECKLIST.md) - Step-by-step manual testing instructions
- [Test Results](docs/testing/) - Test execution results and reports

### Test Results

Test results are saved to:
- `api_test_results.json` - Automated API test results
- `complete_workflow_test_results.json` - Workflow test results

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
