#!/usr/bin/env bash
set -euo pipefail

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example. Update secrets before production use."
else
  echo ".env already exists."
fi

echo "Starting PostgreSQL (pgvector), Redis, and MinIO..."
docker compose up -d
