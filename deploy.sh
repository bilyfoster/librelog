#!/bin/bash
# LibreLog Deployment Script

set -e

echo "🚀 LibreLog Deployment Script"
echo "=============================="

# Check if JAR exists
JAR_FILE="librelog-api/target/librelog-api-0.1.9.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found: $JAR_FILE"
    echo "Building now..."
    cd librelog-api && mvn package -DskipTests -q
    cd ..
fi

# Check if already running
PID=$(pgrep -f "librelog-api.*0.1.9" || true)
if [ -n "$PID" ]; then
    echo "🛑 Stopping existing LibreLog (PID: $PID)..."
    kill $PID 2>/dev/null || true
    sleep 3
fi

# Check for any old LibreLog processes
OLD_PIDS=$(pgrep -f "librelog-api" || true)
if [ -n "$OLD_PIDS" ]; then
    echo "🛑 Stopping old LibreLog processes..."
    echo "$OLD_PIDS" | xargs kill -9 2>/dev/null || true
    sleep 2
fi

echo "📝 Checking database..."
cd librelog-api

# Start the application
echo "🚀 Starting LibreLog v0.1.9..."
nohup java -jar target/librelog-api-0.1.9.jar \
    --spring.profiles.active=prod \
    > ../librelog.log 2>&1 &

NEW_PID=$!
echo $NEW_PID > ../librelog.pid

echo ""
echo "✅ LibreLog started with PID: $NEW_PID"
echo ""
echo "📊 Version: 0.1.9 (with Quick-Start Clock Builder)"
echo "🌐 URL: http://localhost:8080"
echo "📋 Logs: tail -f librelog.log"
echo ""
echo "⏳ Waiting for startup..."

# Wait for startup
for i in {1..30}; do
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✅ Application is ready!"
        echo ""
        echo "🎉 New Features in v0.1.9:"
        echo "   - 🕐 Quick-Start Clock Templates"
        echo "   - 📋 Order → Campaign workflow"
        echo "   - 🎧 Spot creation UI"
        echo ""
        exit 0
    fi
    echo -n "."
    sleep 1
done

echo ""
echo "⚠️  Application may still be starting..."
echo "Check logs: tail -f librelog.log"
