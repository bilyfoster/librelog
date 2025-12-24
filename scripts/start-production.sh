#!/bin/bash
# Production startup script for LibreLog

set -e

echo "=== LibreLog Production Startup ==="
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠ Warning: .env file not found!"
    echo "Creating .env from env.template..."
    cp env.template .env
    echo ""
    echo "⚠ IMPORTANT: Edit .env file and set:"
    echo "  - JWT_SECRET_KEY (must be 32+ characters)"
    echo "  - POSTGRES_PASSWORD (strong password)"
    echo "  - APP_ENV=production"
    echo ""
    read -p "Press Enter after updating .env file to continue..."
fi

# Validate JWT_SECRET_KEY
if grep -q "JWT_SECRET_KEY=your-secret-key" .env || grep -q "JWT_SECRET_KEY=$" .env; then
    echo "❌ ERROR: JWT_SECRET_KEY not set in .env file!"
    echo "Generate a secure key: python3 -c 'import secrets; print(secrets.token_urlsafe(32))'"
    exit 1
fi

JWT_SECRET=$(grep "^JWT_SECRET_KEY=" .env | cut -d'=' -f2)
if [ ${#JWT_SECRET} -lt 32 ]; then
    echo "❌ ERROR: JWT_SECRET_KEY must be at least 32 characters!"
    exit 1
fi

echo "✓ Environment variables validated"
echo ""

# Build Docker images
echo "Building Docker images..."
docker compose build api || docker-compose build api
echo "✓ Images built"
echo ""

# Start services
echo "Starting services..."
docker compose up -d || docker-compose up -d
echo "✓ Services started"
echo ""

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
sleep 5

# Check service status
echo "Service status:"
docker compose ps || docker-compose ps
echo ""

# Run database migrations
echo "Running database migrations..."
docker compose exec -T api alembic upgrade head || docker-compose exec -T api alembic upgrade head
echo "✓ Migrations applied"
echo ""

# Verify API is responding
echo "Verifying API health..."
sleep 3
if docker compose exec -T api curl -f http://localhost:8000/health > /dev/null 2>&1 || \
   docker-compose exec -T api curl -f http://localhost:8000/health > /dev/null 2>&1; then
    echo "✓ API is healthy"
else
    echo "⚠ API health check failed - check logs: docker compose logs api"
fi
echo ""

echo "=== Startup Complete ==="
echo ""
echo "View logs: docker compose logs -f api"
echo "Stop services: docker compose down"
echo ""










