#!/bin/bash
# LibreLog Docker Deployment Script

echo "🐳 LibreLog Docker Deployment"
echo "=============================="
echo ""

# Check if docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Install Docker first."
    exit 1
fi

# Check if docker-compose is available
if command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    COMPOSE_CMD="docker compose"
else
    echo "❌ Docker Compose not found."
    exit 1
fi

echo "🔧 Building new image with Quick-Start features..."
$COMPOSE_CMD build api --no-cache

echo ""
echo "🛑 Stopping existing containers..."
$COMPOSE_CMD down

echo ""
echo "🚀 Starting LibreLog v0.1.9..."
$COMPOSE_CMD up -d

echo ""
echo "⏳ Waiting for services to start..."
sleep 10

# Check health
echo ""
echo "🔍 Checking deployment..."
for i in {1..30}; do
    if curl -s https://log.gayphx.com/api/health > /dev/null 2>&1 || \
       curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo ""
        echo "✅ LibreLog v0.1.9 is deployed and running!"
        echo ""
        echo "🌐 URLs:"
        echo "   - https://log.gayphx.com (production)"
        echo "   - http://localhost:8080 (local)"
        echo ""
        echo "🎉 New in v0.1.9:"
        echo "   - 🕐 Quick-Start Clock Templates"
        echo "   - 📋 Order → Campaign workflow"  
        echo "   - 🎧 Production-ready Spot UI"
        echo ""
        echo "📋 View logs: $COMPOSE_CMD logs -f api"
        exit 0
    fi
    echo -n "."
    sleep 2
done

echo ""
echo "⚠️  Services may still be starting..."
echo "Check logs: $COMPOSE_CMD logs -f api"
