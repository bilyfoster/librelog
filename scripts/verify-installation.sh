#!/bin/bash
# Verification script for LibreLog installation

set -e

echo "=== LibreLog Installation Verification ==="
echo ""

# Check Docker
echo "Checking Docker..."
if command -v docker &> /dev/null; then
    echo "✓ Docker installed: $(docker --version)"
else
    echo "❌ Docker not found"
    exit 1
fi

# Check Docker Compose
echo "Checking Docker Compose..."
if docker compose version &> /dev/null; then
    echo "✓ Docker Compose installed: $(docker compose version)"
    COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    echo "✓ Docker Compose installed: $(docker-compose --version)"
    COMPOSE_CMD="docker-compose"
else
    echo "❌ Docker Compose not found"
    exit 1
fi
echo ""

# Check required files
echo "Checking required files..."
REQUIRED_FILES=(
    "docker-compose.yml"
    "backend/Dockerfile"
    "backend/requirements.txt"
    "env.template"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "❌ $file not found"
        exit 1
    fi
done
echo ""

# Check Python dependencies in requirements.txt
echo "Checking critical Python dependencies..."
CRITICAL_DEPS=("fastapi" "uvicorn" "sqlalchemy" "redis" "zxcvbn" "slowapi" "structlog")
MISSING_DEPS=()

for dep in "${CRITICAL_DEPS[@]}"; do
    if grep -q "^${dep}" backend/requirements.txt || grep -q "^${dep}[" backend/requirements.txt; then
        echo "✓ $dep"
    else
        echo "❌ $dep not found in requirements.txt"
        MISSING_DEPS+=("$dep")
    fi
done

if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
    echo ""
    echo "❌ Missing dependencies: ${MISSING_DEPS[*]}"
    exit 1
fi
echo ""

# Check if services are running
echo "Checking running services..."
if $COMPOSE_CMD ps | grep -q "Up"; then
    echo "✓ Services are running"
    $COMPOSE_CMD ps
else
    echo "⚠ Services not running (this is OK if not started yet)"
fi
echo ""

# Check environment file
echo "Checking environment configuration..."
if [ -f .env ]; then
    echo "✓ .env file exists"
    
    # Check JWT_SECRET_KEY
    if grep -q "^JWT_SECRET_KEY=" .env; then
        JWT_SECRET=$(grep "^JWT_SECRET_KEY=" .env | cut -d'=' -f2)
        if [ ${#JWT_SECRET} -ge 32 ]; then
            echo "✓ JWT_SECRET_KEY is set and valid (${#JWT_SECRET} characters)"
        else
            echo "⚠ JWT_SECRET_KEY is too short (${#JWT_SECRET} characters, need 32+)"
        fi
    else
        echo "⚠ JWT_SECRET_KEY not set in .env"
    fi
    
    # Check POSTGRES_PASSWORD
    if grep -q "^POSTGRES_PASSWORD=" .env && ! grep -q "^POSTGRES_PASSWORD=\$" .env; then
        echo "✓ POSTGRES_PASSWORD is set"
    else
        echo "⚠ POSTGRES_PASSWORD not set in .env"
    fi
else
    echo "⚠ .env file not found - create from env.template"
fi
echo ""

echo "=== Verification Complete ==="
echo ""
echo "Next steps:"
echo "  1. Ensure .env file is configured (copy from env.template)"
echo "  2. Set JWT_SECRET_KEY (32+ characters)"
echo "  3. Set POSTGRES_PASSWORD"
echo "  4. Run: ./scripts/start-production.sh"
echo ""










