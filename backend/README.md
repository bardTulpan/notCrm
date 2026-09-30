# Pipeline CRM — Backend

Production-ready Spring Boot REST API для CRM онлайн-школы. Модульный монолит:
Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway.

## Требования

- JDK 21
- Maven 3.9+ (или используйте обёртку `./mvnw`)
- Docker + Docker Compose (для PostgreSQL)

## Локальный запуск

Команды используют `.env.example` напрямую: создавать или перезаписывать `.env` не нужно. Если есть рабочий `backend/.env`, подставьте его вместо `.env.example` в командах.

### 1. Поднимите PostgreSQL

```bash
cd "/Users/vasa/Documents/Codex Folder/notCrm/backend"
docker compose --env-file .env.example up -d
docker compose --env-file .env.example ps
```

Дождитесь статуса `healthy`. В `.env.example` база доступна на `localhost:5434`.

### 2. Запустите backend в отдельном терминале

```bash
cd "/Users/vasa/Documents/Codex Folder/notCrm/backend"
# Для установленного через Homebrew OpenJDK 21 на macOS
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
set -a
source .env.example
set +a
./mvnw spring-boot:run
```

Если Java 21 уже доступна в `PATH`, первые две строки можно пропустить. Проверьте API:

```bash
curl http://localhost:8080/api/v1/health
# {"status":"ok"}
```

### 3. Запустите frontend в другом терминале

```bash
cd "/Users/vasa/Documents/Codex Folder/notCrm/frontend"
npm run dev
```

Откройте `http://localhost:5173/login`. Если Vite выбрал `5174`, остановите backend и запустите его с `FRONTEND_ORIGIN=http://localhost:5174 ./mvnw spring-boot:run`, чтобы CORS разрешал фактический порт.

Перед запуском проверьте, не работают ли сервисы уже: `lsof -nP -iTCP:5173 -sTCP:LISTEN` и `lsof -nP -iTCP:8080 -sTCP:LISTEN`. Не запускайте второй экземпляр на занятых портах. Не используйте `docker compose down -v`: эта команда удалит том PostgreSQL с локальными данными. Смена `POSTGRES_PASSWORD` в env-файле сама по себе не меняет пароль роли в уже созданном томе.

## Тесты

```bash
./mvnw test
```

Интеграционные тесты используют Testcontainers (PostgreSQL 16), нужен запущенный Docker.

## Миграции и схема

- Flyway-миграции в `src/main/resources/db/migration/` — применяются всегда (локально, в тестах, на проде).
- `V1__initial_schema.sql` — таблицы.
- `V2__seed_development_data.sql` — dev-данные (этапы, когорты, пользователи).
- `V4__student_stage_position.sql` — добавляет `students.stage_position` (ручной порядок карточек в колонке канбана).
- `src/main/resources/db/dev-seed/V3__realistic_cohort_seed_data.sql` — демо-лиды/ученики для локальной разработки.
  Применяется, только если активен Spring-профиль `dev` (см. `SPRING_PROFILES_ACTIVE=dev` в `.env.example`).
  Никогда не подключайте `db/dev-seed` на проде — эта миграция удаляет все существующие лиды/учеников перед
  вставкой демо-данных.
- Hibernate работает в режиме `ddl-auto: validate` (схему создаёт только Flyway).

## Учётные данные для локальной разработки (seed)

| Логин    | Пароль     | Роль     | Статус       |
|----------|------------|----------|--------------|
| admin    | admin123   | ADMIN    | active       |
| anya.t   | curator1   | CURATOR  | active       |
| igor.l   | curator2   | CURATOR  | active       |
| sveta.r  | curator3   | CURATOR  | blocked      |

Пароли хранятся только как BCrypt-хеши в БД. Эти учебные пароли — только для локального
окружения; в production создавайте пользователя с собственным паролем и удалите seed-миграцию.

## Аутентификация

- `POST /api/v1/auth/login` принимает `{ username, password }`, возвращает `accessToken` (JWT, 15 минут)
  и ставит refresh-cookie (HttpOnly, 7 дней).
- Доступ к защищённым маршрутам: заголовок `Authorization: Bearer <accessToken>`.
- Обновление пары: `POST /api/v1/auth/refresh` (refresh token передаётся cookie).

## Роли (RBAC)

- **ADMIN** — полный доступ: лиды, ученики, этапы, когорты, пользователи, статистика.
- **CURATOR** — только свои лиды и ученики; не управляет пользователями/этапами/когортами,
  не переназначает куратора. Попытка доступа к чужому лиду/ученику по ID возвращает `404`.

## Основные API-маршруты

Базовый префикс — `/api/v1`.

| Метод | Маршрут | Доступ |
|-------|---------|--------|
| GET | /health | public |
| POST | /auth/login, /auth/refresh, /auth/logout | public / cookie |
| GET | /auth/me | auth |
| PATCH | /auth/me/password | auth |
| GET/POST | /users | ADMIN |
| GET/PATCH/DELETE | /users/{id} | ADMIN |
| POST | /users/{id}/block, /unblock, /reset-password | ADMIN |
| GET/POST | /pipeline-stages | auth / ADMIN |
| PATCH | /pipeline-stages/{id} | ADMIN |
| PUT | /pipeline-stages/order | ADMIN |
| POST | /pipeline-stages/{id}/archive | ADMIN |
| GET/POST | /cohorts | auth / ADMIN |
| PATCH | /cohorts/{id} | ADMIN |
| POST | /cohorts/{id}/archive | ADMIN |
| GET/POST | /leads | auth |
| GET/PATCH | /leads/{id} | owner or ADMIN |
| POST | /leads/{id}/archive, /restore, /postpone-ping | owner or ADMIN |
| POST | /leads/{id}/convert | owner or ADMIN |
| GET/POST | /students | auth |
| GET/PATCH | /students/{id} | owner or ADMIN |
| POST | /students/{id}/move-stage, /reorder, /pause, /resume | owner or ADMIN |
| POST | /students/{id}/assign-curator | ADMIN |
| GET | /students/{id}/history | owner or ADMIN |
| GET/POST | /students/{id}/comments | owner or ADMIN |
| PATCH/DELETE | /students/{id}/comments/{commentId} | author or ADMIN |
| GET | /stats/overview, /stages, /curators, /overdue-students, /cohorts, /cohorts/{id} | auth (CURATOR видит только свои данные) |

## Основные конфигурационные переменные

| Переменная | Назначение |
|------------|------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | подключение к PostgreSQL |
| `JWT_SECRET` | ключ подписи JWT (>=32 байт) |
| `JWT_ACCESS_TTL_MINUTES` | срок access token |
| `JWT_REFRESH_TTL_DAYS` | срок refresh token |
| `JWT_COOKIE_SECURE` | флаг `Secure` для refresh-cookie |
| `FRONTEND_ORIGIN` | origin фронтенда для CORS |
