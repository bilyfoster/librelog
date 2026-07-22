#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

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

# The version this checkout will build (the <version> that follows the librelog artifactId).
EXPECTED_VERSION="$(awk '/<artifactId>librelog<\/artifactId>/{found=1} found && /<version>/{gsub(/.*<version>|<\/version>.*/,""); print; exit}' pom.xml)"
echo "Building version: ${EXPECTED_VERSION:-unknown}"

echo "[1/2] docker compose build api (Maven + JDK 21 inside image)..."
$COMPOSE_CMD build api

echo "[2/2] docker compose up -d (recreates api with new image; DB volume kept)..."
$COMPOSE_CMD up -d --force-recreate api

echo
echo "Verifying the LOCAL container came up on the new version..."
LOCAL_OK=""
for i in $(seq 1 60); do
    LOCAL_VERSION="$(docker exec librelog-api wget -qO- http://127.0.0.1:8080/api/version 2>/dev/null \
        | sed -n 's/.*"version":"\([^"]*\)".*/\1/p' || true)"
    if [ -n "$LOCAL_VERSION" ]; then LOCAL_OK=1; break; fi
    sleep 2
done
if [ -z "$LOCAL_OK" ]; then
    echo "Local container did not become healthy in 120s. Check logs:"
    echo "  $COMPOSE_CMD logs -f api"
    exit 1
fi
echo "Local container is up, running version $LOCAL_VERSION."
if [ -n "$EXPECTED_VERSION" ] && [ "$LOCAL_VERSION" != "$EXPECTED_VERSION" ]; then
    echo "WARNING: local container reports $LOCAL_VERSION but this checkout is $EXPECTED_VERSION." >&2
    exit 1
fi

echo
echo "Checking what https://log.gayphx.com is serving..."
# Retry for ~40s: right after a recreate, Traefik can take a few seconds to
# re-register the container, and a single immediate curl false-negatives.
PUBLIC_VERSION=""
for i in $(seq 1 10); do
    PUBLIC_VERSION="$(curl -fs --max-time 5 https://log.gayphx.com/api/version 2>/dev/null \
        | sed -n 's/.*"version":"\([^"]*\)".*/\1/p' || true)"
    if [ -n "$PUBLIC_VERSION" ]; then break; fi
    sleep 4
done
if [ -z "$PUBLIC_VERSION" ]; then
    echo "Could not reach https://log.gayphx.com — if this host IS the production server," >&2
    echo "check Traefik; otherwise the public site is unaffected by this deploy." >&2
    exit 1
fi
if [ "$PUBLIC_VERSION" = "$LOCAL_VERSION" ]; then
    echo "OK - https://log.gayphx.com is serving $PUBLIC_VERSION (this deploy is live)."
    exit 0
fi
echo "NOTE: https://log.gayphx.com is serving $PUBLIC_VERSION, but this machine's container runs $LOCAL_VERSION." >&2
echo "The public site is hosted elsewhere (DNS does not point at this host)." >&2
echo "This deploy only updated the LOCAL stack. To update production, run this script on the production server." >&2
exit 1
