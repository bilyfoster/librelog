#!/bin/bash
set -euo pipefail

echo "LibreLog v2 deploy"
echo "=================="

if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD="docker-compose"
elif docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
else
    echo "Docker Compose not found." >&2
    exit 1
fi

echo "[1/4] mvn package..."
mvn -DskipTests -q clean package

echo "[2/4] docker compose down..."
$COMPOSE_CMD down --remove-orphans

echo "[3/4] docker compose build api..."
$COMPOSE_CMD build api

echo "[4/4] docker compose up -d..."
$COMPOSE_CMD up -d

echo
echo "Waiting for /actuator/health..."
for i in $(seq 1 60); do
    if curl -fs -o /dev/null https://log.gayphx.com/actuator/health 2>/dev/null; then
        echo "OK - https://log.gayphx.com is up"
        exit 0
    fi
    sleep 2
done

echo "Did not become healthy in 120s. Check logs:"
echo "  $COMPOSE_CMD logs -f api"
exit 1
