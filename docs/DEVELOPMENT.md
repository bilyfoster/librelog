# LibreLog Development Guide

## Prerequisites

- Docker and Docker Compose
- Node.js 18+ (for local development)
- Python 3.11+ (for local development)

## Local Development Setup

### 1. Clone and Setup

```bash
git clone <repository-url>
cd librelog
cp .cursor/env.template .env
# Edit .env with your LibreTime and AzuraCast API keys
```

### 2. Start Services

```bash
# Start all services
docker-compose up -d

# Or start individual services
docker-compose up -d db redis
docker-compose up -d api
docker-compose up -d frontend
```

### 3. Database Setup

```bash
# Run migrations
docker-compose exec api alembic upgrade head

# Create initial admin user (TODO: implement)
docker-compose exec api python scripts/create_admin.py
```

### 4. Access Applications

- Frontend: http://localhost:3000
- API: http://localhost:8000
- API Docs: http://localhost:8000/docs
- Database: localhost:5432

## Development Workflow

### Backend Development

```bash
# Install dependencies
cd backend
pip install -r requirements.txt

# Run tests
pytest

# Run with coverage
pytest --cov=backend --cov-report=html

# Format code
black .
isort .

# Run linting
flake8 .
```

### Frontend Development

```bash
# Install dependencies
cd frontend
npm install

# Run development server
npm run dev

# Run tests
npm test

# Run tests with UI
npm run test:ui

# Build for production
npm run build
```

## Project Structure

```
librelog/
├── backend/                 # FastAPI backend
│   ├── models/             # SQLAlchemy models
│   ├── routers/            # API routes
│   ├── services/           # Business logic
│   ├── integrations/       # External API clients
│   ├── auth/               # Authentication
│   ├── tasks/              # Celery tasks
│   └── alembic/            # Database migrations
├── frontend/               # React frontend
│   ├── src/
│   │   ├── components/     # Reusable components
│   │   ├── pages/          # Page components
│   │   ├── contexts/       # React contexts
│   │   ├── hooks/          # Custom hooks
│   │   ├── utils/          # Utility functions
│   │   └── types/          # TypeScript types
├── shared/                 # Shared types and schemas
├── tests/                  # Integration tests
├── docs/                   # Documentation
└── docker-compose.yml      # Docker services
```

## API Development

### Adding New Endpoints

1. Create model in `backend/models/`
2. Create router in `backend/routers/`
3. Add service logic in `backend/services/`
4. Add tests in `tests/`
5. Update API schema in `.cursor/api-schema.yaml`

### Database Changes

1. Modify model in `backend/models/`
2. Create migration: `alembic revision --autogenerate -m "description"`
3. Apply migration: `alembic upgrade head`
4. Update tests

## Testing

### Backend Tests

```bash
# Run all tests
pytest

# Run specific test file
pytest tests/test_auth.py

# Run with coverage
pytest --cov=backend --cov-report=term-missing
```

### Frontend Tests

```bash
# Run all tests
npm test

# Run tests in watch mode
npm test -- --watch

# Run tests with coverage
npm run test:coverage
```

## Deployment

### Production Build

```bash
# Build frontend
cd frontend
npm run build

# Build Docker images
docker-compose build

# Deploy
docker-compose -f docker-compose.prod.yml up -d
```

### Environment Variables

Required environment variables for production:

- `LIBRETIME_URL`: LibreTime API URL
- `LIBRETIME_API_KEY`: LibreTime API key
- `AZURACAST_URL`: AzuraCast API URL
- `AZURACAST_API_KEY`: AzuraCast API key
- `POSTGRES_URI`: PostgreSQL connection string
- `JWT_SECRET_KEY`: JWT signing key
- `REDIS_URL`: Redis connection string

## Troubleshooting

### Common Issues

1. **Database connection errors**: Check PostgreSQL is running and credentials are correct
2. **API not responding**: Check FastAPI logs with `docker-compose logs api`
3. **Frontend build errors**: Check Node.js version and dependencies
4. **Migration errors**: Check database schema and run `alembic upgrade head`

### Logs

```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs api
docker-compose logs frontend
docker-compose logs db
```

## Contributing

1. Create feature branch from `main`
2. Make changes with tests
3. Run full test suite
4. Submit pull request
5. Code review and merge
