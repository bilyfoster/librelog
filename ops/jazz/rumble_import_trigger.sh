#!/bin/bash
set -euo pipefail

# Executed on the Jazz/LibreTime server after Rumble finishes an SFTP upload.
# Arg 1: absolute path to the uploaded audio file.
FILE_TO_IMPORT="${1:-}"

if [ -z "$FILE_TO_IMPORT" ]; then
    echo "ERROR: Target sound file path is required."
    exit 2
fi

if [ -f "$FILE_TO_IMPORT" ]; then
    libretime-api import "$FILE_TO_IMPORT" --copy
    echo "SUCCESS: File imported to system library."
    exit 0
else
    echo "ERROR: Target sound file not found."
    exit 1
fi
