#!/usr/bin/env bash
# Dumps the pipeline-crm Postgres database (running in the pipeline-crm-postgres
# Docker container) to a timestamped, gzip-compressed .sql file under
# backend/backups/, and prunes dumps older than $RETENTION_DAYS.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$BACKEND_DIR/backups"
CONTAINER_NAME="pipeline-crm-postgres"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

ENV_FILE="$BACKEND_DIR/.env"
[ -f "$ENV_FILE" ] || ENV_FILE="$BACKEND_DIR/.env.example"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if ! docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" >/dev/null 2>&1; then
  echo "backup-db: container '$CONTAINER_NAME' is not running, skipping backup" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="$BACKUP_DIR/${POSTGRES_DB:-pipeline_crm}_${TIMESTAMP}.sql.gz"
TMP_FILE="${OUT_FILE}.tmp"

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" "$CONTAINER_NAME" \
  pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "$TMP_FILE"

mv "$TMP_FILE" "$OUT_FILE"
echo "backup-db: wrote $OUT_FILE"

find "$BACKUP_DIR" -name '*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete
