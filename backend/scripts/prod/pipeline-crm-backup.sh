#!/usr/bin/env bash
# Daily pg_dump of pipeline_crm. Runs as the postgres user (systemd timer).
# Keeps RETENTION_DAYS locally; if an rclone remote named "yadisk" is configured, also uploads there.
set -euo pipefail

DB="pipeline_crm"
DIR="/var/backups/pipeline-crm"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
REMOTE="${REMOTE:-yadisk:}"

mkdir -p "$DIR"
OUT="$DIR/${DB}_$(date +%Y%m%d_%H%M%S).dump"

pg_dump -Fc "$DB" > "$OUT.tmp"
pg_restore -l "$OUT.tmp" > /dev/null
mv "$OUT.tmp" "$OUT"
echo "backup ok: $OUT ($(du -h "$OUT" | cut -f1))"

find "$DIR" -name '*.dump' -mtime "+$RETENTION_DAYS" -delete

REMOTES="$(rclone listremotes 2>/dev/null || true)"
if [[ "$REMOTES" == *"yadisk:"* ]]; then
  rclone copy "$OUT" "$REMOTE" && echo "uploaded to $REMOTE"
  rclone delete "$REMOTE" --min-age "${RETENTION_DAYS}d"
else
  echo "rclone remote 'yadisk' not configured - local backup only" >&2
fi
