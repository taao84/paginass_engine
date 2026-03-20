#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# start.sh — Start the Paginass application (database + Spring Boot)
# Usage: ./deployment/start.sh [--skip-build]
# -----------------------------------------------------------------------------
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SKIP_BUILD=false

for arg in "$@"; do
  case $arg in
    --skip-build) SKIP_BUILD=true ;;
  esac
done

# ── 1. Start the database ────────────────────────────────────────────────────
echo ">>> Removing any existing containers..."
docker compose -f "$PROJECT_DIR/deployment/docker-compose.yaml" down --remove-orphans 2>/dev/null || true
docker rm -f postgres 2>/dev/null || true

echo ">>> Starting PostgreSQL via Docker Compose..."
docker compose -f "$PROJECT_DIR/deployment/docker-compose.yaml" up -d

# ── 2. Wait for PostgreSQL to accept connections ─────────────────────────────
echo ">>> Waiting for PostgreSQL to be ready..."
RETRIES=30
until docker exec postgres pg_isready -U paginass -d paginass -q; do
  RETRIES=$((RETRIES - 1))
  if [ "$RETRIES" -le 0 ]; then
    echo "ERROR: PostgreSQL did not become ready in time." >&2
    exit 1
  fi
  sleep 2
done
echo ">>> PostgreSQL is ready."

# ── 3. Build the application ─────────────────────────────────────────────────
if [ "$SKIP_BUILD" = false ]; then
  echo ">>> Building application (skipping tests)..."
  "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" build -x test
fi

# ── 4. Start Spring Boot ─────────────────────────────────────────────────────
echo ">>> Starting Paginass..."
export DB_USERNAME=paginass
export DB_PASSWORD=paginass
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/paginass
export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-change-me}"
export GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-change-me}"
export GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-change-me}"
export GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-change-me}"

"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" bootRun


