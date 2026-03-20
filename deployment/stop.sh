#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# stop.sh — Stop the Paginass application database container
# Usage: ./deployment/stop.sh [--volumes]
# -----------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
REMOVE_VOLUMES=false

for arg in "$@"; do
  case $arg in
    --volumes) REMOVE_VOLUMES=true ;;
  esac
done

if [ "$REMOVE_VOLUMES" = true ]; then
  echo ">>> Stopping PostgreSQL and removing volumes (data will be lost)..."
  docker compose -f "$PROJECT_DIR/deployment/docker-compose.yaml" down --volumes
else
  echo ">>> Stopping PostgreSQL (data volume preserved)..."
  docker compose -f "$PROJECT_DIR/deployment/docker-compose.yaml" down
fi

echo ">>> Done."
