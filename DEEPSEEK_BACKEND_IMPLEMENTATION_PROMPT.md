# Промпт для DeepSeek V4 Pro

Ты работаешь как самостоятельный senior backend-инженер в существующей директории проекта. Твоя задача: реализовать production-ready backend для Pipeline CRM, проверить его локально и оставить проект в рабочем состоянии. После завершения backend НЕ создавай frontend: архитектуру и реализацию frontend выполнит другой инженер на основе готового API.

## Исходные документы

Сначала полностью прочитай:

1. `README.md` - описание бизнес-логики прототипа.
2. `pipeline-crm-prototype.html` - UX-прототип и источник деталей поведения.
3. `DEVELOPMENT_BACKLOG.md` - обязательный технический backlog.

`DEVELOPMENT_BACKLOG.md` является главным источником требований к реализации. Выполни полностью задачи Спринта 1 и Спринта 2. Не выполняй Спринт 3.

## Цель

Создать папку `backend` с работающим Spring Boot REST API для CRM:

- Java 21;
- Spring Boot 3;
- Maven;
- Spring Web;
- Spring Data JPA;
- Spring Security;
- PostgreSQL 16;
- Flyway;
- Bean Validation;
- Lombok;
- JWT access/refresh authentication;
- server-side RBAC для ролей `ADMIN` и `CURATOR`.

Система рассчитана максимум на 10 одновременных пользователей. Делай модульный монолит, не добавляй микросервисы, Kafka, Redis, Kubernetes или усложнения без прямой необходимости.

## Непереговорные технические правила

- Реализуй все backend-задачи `S1-01` ... `S1-08` и `S2-01` ... `S2-09` из `DEVELOPMENT_BACKLOG.md`.
- Используй UUID для ID бизнес-сущностей.
- REST API имеет префикс `/api/v1`.
- Не возвращай JPA Entity из API. Создавай request/response DTO.
- Используй Flyway для каждой миграции. Hibernate: `ddl-auto=validate`.
- Используй `timestamptz` в PostgreSQL и `Instant`/`OffsetDateTime` в Java. Все даты API - ISO 8601 UTC.
- Используй транзакции для конвертации лида и перехода ученика между этапами.
- Soft delete обязателен для пользователей, лидов, учеников и комментариев. Не применяй физическое удаление в бизнес-API.
- Пароли - исключительно BCrypt hash. Никогда не отдавай, не логируй и не записывай в аудит пароли, JWT или `passwordHash`.
- JWT access token возвращается в JSON и действует 15 минут. Refresh token действует 7 дней и хранится только в `HttpOnly`, `SameSite=Lax` cookie. Флаг `Secure` должен конфигурироваться для dev/prod.
- CORS должен разрешать только frontend origin из переменной окружения.
- Не полагайся на frontend для проверки прав. Каждая операция проверяет роль и владельца данных на сервере.
- Для CURATOR попытка читать или менять чужого лида/ученика по ID должна отвечать `404`, а не раскрывать существование сущности через `403`.
- Архивированные этапы нельзя назначать ученику. Не удаляй этапы с историей физически.
- Когорта - независимая редактируемая отчетная метка, не вычисляемая на frontend.
- Health status ученика не хранить в БД: вычислять на backend по норме этапа, дате входа и паузе.
- В аудит не включай чувствительные поля.
- Не изменяй `pipeline-crm-prototype.html`, `README.md` или `DEVELOPMENT_BACKLOG.md` без крайней необходимости.

## RBAC

### ADMIN

- Полный доступ ко всем лидам, ученикам, статистике, этапам, когортам и пользователям.
- Создает, блокирует, разблокирует и soft-delete пользователей.
- Меняет этапы, когорты и куратора ученика.

### CURATOR

- Видит и меняет только свои лиды и учеников.
- Новый лид автоматически назначается текущему куратору.
- Может конвертировать только свой лид и назначает созданного ученика только себе.
- Может двигать своих учеников по этапам, ставить на паузу, снимать с паузы, добавлять комментарии.
- Не меняет пользователей, этапы, когорты, не переназначает учеников.
- В статистике видит только свои данные и не получает показатели других кураторов.

## Обязательные доменные сущности

Реализуй следующие таблицы, Entity, repository, сервисы, DTO и API по backlog:

```text
users
pipeline_stages
cohorts
leads
lead_notes
students
student_notes
student_stage_history
student_curator_history
student_comments
audit_log
```

Важно:

- При конвертации лида в ученика копируй заметки лида в заметки ученика.
- Для текущего этапа есть одна открытая `student_stage_history` запись с `exited_at = null`.
- При переходе между этапами закрывай старую историю и создавай новую в одной транзакции.
- Переназначение куратора создает `student_curator_history`.
- Пауза не создает новую историю этапа.
- Нельзя заблокировать или удалить последнего активного ADMIN.
- Нельзя удалить куратора, у которого есть активные ученики: сначала их требуется переназначить.

## Обязательные API

Полный контракт, DTO и правила смотри в `DEVELOPMENT_BACKLOG.md`. Реализуй минимум следующие маршруты:

```text
GET    /api/v1/health

POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
GET    /api/v1/auth/me

GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
POST   /api/v1/users/{id}/block
POST   /api/v1/users/{id}/unblock
POST   /api/v1/users/{id}/reset-password
DELETE /api/v1/users/{id}

GET    /api/v1/pipeline-stages
POST   /api/v1/pipeline-stages
PATCH  /api/v1/pipeline-stages/{id}
PUT    /api/v1/pipeline-stages/order
POST   /api/v1/pipeline-stages/{id}/archive

GET    /api/v1/cohorts
POST   /api/v1/cohorts
PATCH  /api/v1/cohorts/{id}
POST   /api/v1/cohorts/{id}/archive

GET    /api/v1/leads
POST   /api/v1/leads
GET    /api/v1/leads/{id}
PATCH  /api/v1/leads/{id}
POST   /api/v1/leads/{id}/archive
POST   /api/v1/leads/{id}/restore
POST   /api/v1/leads/{id}/postpone-ping
POST   /api/v1/leads/{id}/convert

GET    /api/v1/students
POST   /api/v1/students
GET    /api/v1/students/{id}
PATCH  /api/v1/students/{id}
POST   /api/v1/students/{id}/move-stage
POST   /api/v1/students/{id}/pause
POST   /api/v1/students/{id}/resume
POST   /api/v1/students/{id}/assign-curator
GET    /api/v1/students/{id}/history
GET    /api/v1/students/{id}/comments
POST   /api/v1/students/{id}/comments
PATCH  /api/v1/students/{id}/comments/{commentId}
DELETE /api/v1/students/{id}/comments/{commentId}

GET    /api/v1/stats/overview
GET    /api/v1/stats/stages
GET    /api/v1/stats/curators
GET    /api/v1/stats/overdue-students
GET    /api/v1/stats/cohorts
GET    /api/v1/stats/cohorts/{id}
```

## Обязательная структура backend

Используй понятную модульную структуру, близкую к этой:

```text
backend/
  pom.xml
  mvnw
  mvnw.cmd
  compose.yaml
  .env.example
  README.md
  src/
    main/
      java/com/pipeline/crm/
        CrmApplication.java
        config/
        common/
          exception/
          validation/
          mapper/
        security/
        auth/
        user/
        pipeline/
        lead/
        student/
        cohort/
        statistics/
        audit/
      resources/
        application.yml
        application-dev.yml
        db/migration/
    test/
```

Не делай один большой `service` или `controller`; группируй код по доменным модулям. Не создавай избыточные generic-абстракции.

## Требования к тестированию

Напиши и запусти полезные автоматические тесты. Минимум покрыть:

1. Контекст приложения и health endpoint.
2. Flyway миграции на чистой PostgreSQL-совместимой тестовой БД. Предпочтительно использовать Testcontainers PostgreSQL; если окружение не позволяет Docker в тестах, объясни это в отчете и оставь интеграционные тесты запускаемыми при наличии Docker.
3. Login: успех, неверный пароль, заблокированный пользователь.
4. RBAC: CURATOR не может управлять пользователями, этапами и когортами.
5. Владение: CURATOR получает `404` при попытке открыть или изменить чужой лид и ученика.
6. Конвертация лида: создает ученика, открытую историю этапа, копирует заметки, меняет статус лида; повтор возвращает `409`.
7. Переход этапа: закрывает старую историю, создает новую.
8. Нельзя назначить ученика на архивный этап.
9. Статистика CURATOR не содержит данных других кураторов.
10. Аудит создается для критичных операций и не содержит чувствительных данных.

## Требования к проверке

Перед завершением обязательно:

1. Создай `backend/.env` из `.env.example` только локально, если нужно для проверки; `.env` не должен попасть в git.
2. Подними PostgreSQL через `docker compose up -d` из папки `backend`.
3. Запусти миграции через старт приложения или тесты.
4. Запусти полный набор тестов: `./mvnw test`.
5. Запусти приложение и проверь минимум health endpoint, login seed-admin и один защищенный маршрут через HTTP-запросы.
6. Исправь все обнаруженные ошибки, затем повтори проверку.
7. Добавь в `backend/README.md` краткую, точную инструкцию: требования, настройка `.env`, запуск PostgreSQL, запуск приложения, запуск тестов, URL API, данные seed-admin для локального окружения. Не добавляй открытый пароль в Flyway-миграции; учебный пароль допустим только в README для локального seed-пользователя.

Если Docker, Maven Wrapper, Java 21 или сеть недоступны в окружении, все равно реализуй код полностью. В конце явно перечисли, какая конкретно проверка не была запущена и почему. Не утверждай, что тест пройден, если команда реально не завершилась успешно.

## Что вернуть в финальном отчете

После завершения напиши короткий отчет:

1. Какие файлы/модули созданы.
2. Какие задачи backlog выполнены.
3. Команды проверки и их реальный результат.
4. Seed-учетные данные для локального запуска, если они созданы.
5. Известные ограничения или непрошедшие проверки, если есть.
6. Точный список реализованных API-маршрутов.

Не начинай frontend. Остановись после полностью работающего и проверенного backend.
