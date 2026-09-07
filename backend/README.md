# Pipeline CRM — Backend

Production-ready Spring Boot REST API для CRM онлайн-школы. Модульный монолит:
Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway.

## Требования

- JDK 21
- Maven 3.9+ (или используйте обёртку `./mvnw`)
- Docker + Docker Compose (для PostgreSQL)

## Локальный запуск

### 1. Настройте окружение

```bash
cp .env.example .env
```

Отредактируйте `.env` под себя. Для локальной разработки уже заданы значения по умолчанию
(БД на порту `5434`, чтобы не конфликтовать с другими проектами на `5432`).

### 2. Поднимите PostgreSQL

```bash
docker compose up -d
```

### 3. Запустите приложение

Переменные приложения Spring Boot читает из окружения (не из `.env` напрямую), поэтому
экспортируйте их перед запуском:

```bash
set -a && source .env && set +a
./mvnw spring-boot:run
```

Приложение стартует на порту `8080`. Если порт занят, переопределите:
`SERVER_PORT=8081 ./mvnw spring-boot:run`.

### 4. Проверьте

```bash
curl http://localhost:8080/api/v1/health
# {"status":"ok"}
```

## Тесты

```bash
./mvnw test
```

Интеграционные тесты используют Testcontainers (PostgreSQL 16), нужен запущенный Docker.

## Миграции и схема

- Flyway-миграции в `src/main/resources/db/migration/`.
- `V1__initial_schema.sql` — таблицы.
- `V2__seed_development_data.sql` — dev-данные (этапы, когорты, пользователи).
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
| POST | /students/{id}/move-stage, /pause, /resume | owner or ADMIN |
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
