#!/bin/bash
# Check LibreLog Deployment Status

echo "🔍 LibreLog Deployment Status"
echo "=============================="
echo ""

# Check if JAR exists
JAR_FILE="librelog-api/target/librelog-api-0.1.9.jar"
if [ -f "$JAR_FILE" ]; then
    echo "✅ JAR file exists: $JAR_FILE"
    echo "📅 Build time: $(stat -c %y $JAR_FILE 2>/dev/null || stat -f %Sm $JAR_FILE 2>/dev/null || echo 'Unknown')"
    
    # Check version in JAR
    VERSION=$(unzip -p $JAR_FILE META-INF/MANIFEST.MF 2>/dev/null | grep Implementation-Version | cut -d: -f2 | tr -d ' ' || echo "unknown")
    echo "📊 Version in JAR: $VERSION"
    
    # Check for quickstart module
    if jar tf $JAR_FILE | grep -q "clock-builder-quickstart.js"; then
        echo "✅ Quick-Start module: INCLUDED"
    else
        echo "❌ Quick-Start module: MISSING"
    fi
else
    echo "❌ JAR file NOT FOUND: $JAR_FILE"
fi

echo ""
echo "🖥️  Process Status:"
PID=$(pgrep -f "librelog-api" || true)
if [ -n "$PID" ]; then
    echo "✅ LibreLog is RUNNING (PID: $PID)"
    ps -p $PID -o pid,etime,cmd | tail -1
    
    echo ""
    echo "🌐 Health Check:"
    if curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "✅ Application responding on http://localhost:8080"
        
        # Check version endpoint
        VERSION_RESP=$(curl -s http://localhost:8080/api/version 2>/dev/null || echo "unknown")
        echo "📊 API Version: $VERSION_RESP"
    else
        echo "❌ Application NOT responding (may still be starting)"
    fi
else
    echo "❌ LibreLog is NOT RUNNING"
    echo "   Run: ./deploy.sh"
fi

echo ""
echo "🗄️  Database:"
if pgrep -x "postgres" > /dev/null; then
    echo "✅ PostgreSQL is running"
else
    echo "❌ PostgreSQL is NOT running"
fi

echo ""
echo "🔧 Recommended Actions:"
if [ -z "$PID" ]; then
    echo "   1. Start the application: ./deploy.sh"
fi
if [ -f "librelog.log" ]; then
    echo "   2. View logs: tail -f librelog.log"
fi
echo "   3. Open browser: http://localhost:8080"
