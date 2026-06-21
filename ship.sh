#!/usr/bin/env bash
# Commit all changes, push the current branch, then deploy (Docker).
# Usage: ./ship.sh "Your commit message"
# Bump dashboard.html ?v= on app.css / app.js when you change those files so browsers reload them.
set -euo pipefail
cd "$(dirname "$0")"
MSG="${1:-Update librelog}"

if ! command -v docker >/dev/null 2>&1; then
    echo "docker not found" >&2
    exit 1
fi

if git rev-parse --git-dir >/dev/null 2>&1; then
    git add -A
    if git diff --cached --quiet && git diff --quiet; then
        echo "No git changes to commit."
    else
        git commit -m "$MSG"
    fi
    branch="$(git rev-parse --abbrev-ref HEAD)"
    git push -u origin "$branch" || echo "Warning: git push failed; deploying local build anyway." >&2
else
    echo "Not a git repo; skipping commit/push." >&2
fi

exec ./deploy-docker.sh
