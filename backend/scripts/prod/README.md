# Бэкапы БД на проде

Сервер: Postgres 18 (нативный, не Docker), база `pipeline_crm`. Раз в сутки (~03:00 UTC) systemd timer
запускает `pipeline-crm-backup.sh` от пользователя `postgres`:

1. `pg_dump -Fc` в `/var/backups/pipeline-crm/` (права 700), проверка `pg_restore -l`.
2. Локально хранится 30 дней (`RETENTION_DAYS`).
3. Если в rclone настроен remote `yadisk` (crypt поверх WebDAV Яндекс Диска), дамп отправляется туда
   в зашифрованном виде, старше 30 дней удаляется.

Секреты (пароль приложения Яндекса, пароли шифрования rclone) в репозитории не хранятся: только в
`/var/lib/postgresql/.config/rclone/rclone.conf` на сервере и в менеджере паролей владельца.

## Установка / обновление на сервере

```bash
scp backend/scripts/prod/pipeline-crm-backup.* root@<host>:/tmp/
ssh root@<host>
install -m 755 /tmp/pipeline-crm-backup.sh /usr/local/bin/
install -m 644 /tmp/pipeline-crm-backup.service /tmp/pipeline-crm-backup.timer /etc/systemd/system/
install -d -m 700 -o postgres -g postgres /var/backups/pipeline-crm
systemctl daemon-reload && systemctl enable --now pipeline-crm-backup.timer
systemctl start pipeline-crm-backup.service && journalctl -u pipeline-crm-backup -n 10
```

## Настройка rclone (один раз, интерактивно, под пользователем postgres)

```bash
apt install -y rclone
sudo -u postgres rclone config
```

- `yadisk-raw`: тип `webdav`, url `https://webdav.yandex.ru`, vendor `other`, логин Яндекса,
  пароль приложения «Файлы WebDAV» (id.yandex.ru/security/app-passwords).
- `yadisk`: тип `crypt`, remote `yadisk-raw:pipeline-crm-backups`, пароли сгенерировать (128 бит) и
  сохранить в менеджере паролей.

## Восстановление

```bash
sudo -u postgres rclone copy yadisk: /tmp/restore     # или взять файл из /var/backups/pipeline-crm
sudo -u postgres createdb pipeline_crm_restore
sudo -u postgres pg_restore -d pipeline_crm_restore --no-owner /tmp/restore/<файл>.dump
```

Перед заменой боевой базы остановите `pipeline-crm` (`systemctl stop pipeline-crm`).

## Локальная разработка

`backend/scripts/backup-db.sh` делает дамп локального Docker-Postgres в `backend/backups/`
(не коммитится).
