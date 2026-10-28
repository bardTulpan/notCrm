#!/usr/bin/env bash
# Emails the last backup log lines via Yandex SMTP. Triggered by OnFailure= of pipeline-crm-backup.service.
# Pass "test" as $1 to send a test message. Credentials: /etc/pipeline-crm-backup-alert.netrc (600).
set -euo pipefail
source /etc/pipeline-crm-backup-alert.conf   # MAIL_FROM, MAIL_TO

if [ "${1:-}" = "test" ]; then
  SUBJ="[pipeline-crm] Тест уведомлений о бэкапе"
else
  SUBJ="[pipeline-crm] Бэкап БД упал на $(hostname)"
fi

{
  printf 'From: %s\r\nTo: %s\r\nSubject: =?UTF-8?B?%s?=\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n' \
    "$MAIL_FROM" "$MAIL_TO" "$(printf '%s' "$SUBJ" | base64 -w0)"
  echo "Последние строки лога pipeline-crm-backup:"
  echo
  journalctl -u pipeline-crm-backup -n 30 --no-pager -o short-iso
} | curl -sS --ssl-reqd --url "smtps://smtp.yandex.ru:465" \
      --netrc-file /etc/pipeline-crm-backup-alert.netrc \
      --mail-from "$MAIL_FROM" --mail-rcpt "$MAIL_TO" --upload-file -
