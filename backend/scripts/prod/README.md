# Бэкапы БД на проде

Сервер: Postgres 18 (нативный, не Docker), база `pipeline_crm`. Раз в сутки (~03:00 UTC) systemd timer
запускает `pipeline-crm-backup.sh` от пользователя `postgres`:

1. `pg_dump -Fc` в `/var/backups/pipeline-crm/` (права 700), проверка `pg_restore -l`.
2. Локально хранится 30 дней (`RETENTION_DAYS`).
3. Если в rclone настроен remote `yadisk` (crypt поверх Яндекс Диска), дамп отправляется туда
   в зашифрованном виде, старше 30 дней удаляется.
4. Если сервис упал (дамп или загрузка), `OnFailure=` запускает `pipeline-crm-backup-alert.service`,
   который шлёт письмо с последними строками лога через SMTP Яндекс Почты.

Секреты в репозитории не хранятся:
- rclone (OAuth-токен, client secret, пароли шифрования): `/var/lib/postgresql/.config/rclone/rclone.conf`;
- SMTP: `/etc/pipeline-crm-backup-alert.netrc` (600, root), адреса — `/etc/pipeline-crm-backup-alert.conf`
  (`MAIL_FROM`, `MAIL_TO`);
- пароли шифрования rclone crypt дополнительно — в менеджере паролей владельца.

## Установка / обновление на сервере

```bash
scp backend/scripts/prod/pipeline-crm-backup* root@<host>:/tmp/
ssh root@<host>
install -m 755 /tmp/pipeline-crm-backup.sh /tmp/pipeline-crm-backup-alert.sh /usr/local/bin/
install -m 644 /tmp/pipeline-crm-backup.service /tmp/pipeline-crm-backup-alert.service \
  /tmp/pipeline-crm-backup.timer /etc/systemd/system/
install -d -m 700 -o postgres -g postgres /var/backups/pipeline-crm
systemctl daemon-reload && systemctl enable --now pipeline-crm-backup.timer
systemctl start pipeline-crm-backup.service && journalctl -u pipeline-crm-backup -n 10
```

## Яндекс Диск (rclone, один раз)

WebDAV на бесплатном тарифе Яндекса закрыт (`402 Payment Required`, с 2026-10-23), а общее OAuth-приложение
rclone получает `410 Gone`. Работает только **своё OAuth-приложение**:

1. https://oauth.yandex.ru/client/new → «Для авторизации пользователей», платформа «Веб-сервисы»,
   Redirect URI `http://127.0.0.1:53682/`, доступы `cloud_api:disk.read`, `cloud_api:disk.write`,
   `cloud_api:disk.info`. Получить ClientID и Client secret.
2. На машине с браузером: `rclone authorize yandex <client_id> <client_secret>` — выдаст JSON-токен.
3. На сервере (`apt install -y rclone`, всё под `sudo -u postgres`):
   - `yadisk-raw`: `rclone config update yadisk-raw client_id <id> client_secret <secret> token '<json>' config_refresh_token false --non-interactive`
     (тип `yandex`; при первом создании — `rclone config create yadisk-raw yandex ...`);
   - `yadisk`: тип `crypt`, remote `yadisk-raw:pipeline-crm-backups`, пароли сгенерировать и сохранить.
4. Проверка: `sudo -u postgres rclone lsd yadisk-raw:`.

## Уведомления на почту (один раз)

1. Яндекс Почта → Настройки → Почтовые программы: включить IMAP и «Пароли приложений и OAuth-токены».
2. https://id.yandex.ru/security/app-passwords → пароль приложения типа «Почта».
3. На сервере (от root, `umask 077`):
   ```bash
   printf 'machine smtp.yandex.ru login <from> password <app-password>\n' > /etc/pipeline-crm-backup-alert.netrc
   printf 'MAIL_FROM=<from>\nMAIL_TO=<to>\n' > /etc/pipeline-crm-backup-alert.conf
   /usr/local/bin/pipeline-crm-backup-alert.sh test   # тестовое письмо
   ```

Письмо не придёт, если таймер вообще не запустился (сервер выключен) — это отдельный случай.

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
