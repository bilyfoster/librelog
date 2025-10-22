#!/bin/bash

# LibreLog Deployment Script
# This script helps deploy LibreLog Alpha to your server

set -e

echo "🎵 LibreLog Alpha Deployment Script"
echo "=================================="

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "📝 Creating .env file from template..."
    cp .cursor/env.template .env
    echo "✅ .env file created. Please edit it with your API keys."
    echo ""
    echo "Required environment variables:"
    echo "- LIBRETIME_URL: Your LibreTime API URL"
    echo "- LIBRETIME_API_KEY: Your LibreTime API key"
    echo "- AZURACAST_URL: Your AzuraCast API URL"
    echo "- AZURACAST_API_KEY: Your AzuraCast API key"
    echo ""
    echo "Edit .env file and run this script again."
    exit 0
fi

# Check if API keys are configured
if grep -q "changeme" .env; then
    echo "⚠️  API keys not configured in .env file."
    echo "Please edit .env with your actual API keys and run again."
    exit 1
fi

echo "🚀 Starting LibreLog services..."

# Start services
docker-compose up -d

echo "⏳ Waiting for services to start..."
sleep 10

# Check if services are running
echo "🔍 Checking service status..."
docker-compose ps

echo ""
echo "✅ LibreLog Alpha is now running!"
echo ""
echo "🌐 Access URLs:"
echo "- Frontend: http://localhost:3000"
echo "- API: http://localhost:8000"
echo "- API Docs: http://localhost:8000/docs"
echo "- Setup Page: http://localhost:3000/setup"
echo ""
echo "🔑 Initial Setup:"
echo "1. Go to http://localhost:3000/setup"
echo "2. Create your admin user"
echo "3. Verify API key configuration"
echo "4. Login at http://localhost:3000/login"
echo ""
echo "📋 Default credentials (change after first login):"
echo "- Username: admin"
echo "- Password: admin123"
echo ""
echo "📚 For more information, see README.md"
