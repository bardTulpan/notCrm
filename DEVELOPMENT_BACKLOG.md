# Backlog разработки Pipeline CRM

Этот документ является самостоятельным заданием для реализации production-ready CRM. Не переносить данные и авторизацию из `pipeline-crm-prototype.html`: файл является UX-прототипом. Реализация должна использовать настоящий backend, PostgreSQL и серверную проверку прав.

## Общие правила

- Backend: Java 21, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Spring Security, PostgreSQL, Flyway, Bean Validation, Lombok.
- Frontend: React, TypeScript, Vite, Tailwind CSS, Axios, React Router.
- API имеет базовый путь `/api/v1`.
- Первичные ключи бизнес-сущностей: UUID.
- Все API-ответы используют DTO; JPA Entity не возвращать из контроллеров.
- Даты API: ISO 8601 с timezone. В PostgreSQL: `timestamptz`.
- Hibernate не управляет схемой: `spring.jpa.hibernate.ddl-auto=validate`.
- Каждое изменение схемы оформлять отдельной Flyway-миграцией в `db/migration`.
- Пароли сохранять только как BCrypt-хеши. Никогда не возвращать, не логировать и не помещать в аудит пароли, токены или `passwordHash`.
- Критичные сущности удалять мягко через `deleted_at`; не выполнять физическое удаление из бизнес-API.
- Сервер является единственным источником прав: скрытие кнопок на frontend не заменяет RBAC-проверку на backend.

## Роли

### ADMIN

- Полный доступ ко всем лидам, ученикам, этапам, когортам и статистике.
- Управляет пользователями: создание, изменение, блокировка, сброс пароля, soft delete.
- Управляет глобальными этапами и когортами.
- Может переназначать куратора ученику.
- Может архивировать и восстанавливать лиды.

### CURATOR

- Видит и ведет только лиды, назначенные ему.
- При создании лида автоматически становится его куратором.
- Видит и ведет только учеников, назначенных ему.
- Может перемещать своих учеников по этапам, ставить на паузу, снимать с паузы и добавлять комментарии.
- Не меняет настройки этапов, когорт и пользователей.
- Не переназначает куратора.
- Не получает данные других кураторов даже при прямом вызове API.
- Не удаляет критичные данные безвозвратно.

## Спринт 1. База данных и backend-каркас

### S1-01. Создать Spring Boot проект

Создать Maven-проект в папке `backend`.

Подключить зависимости:

- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Flyway
- Validation
- Lombok
- Spring Boot Test

Создать пакеты:

```text
com.pipeline.crm
  config
  common
  user
  pipeline
  lead
  student
  cohort
  statistics
  audit
```

Создать `GET /api/v1/health`, который возвращает `200 OK` и `{ "status": "ok" }`.

Готово, если:

- `./mvnw test` проходит.
- Приложение запускается на порту `8080`.
- Health endpoint доступен без БД-ошибок.

### S1-02. Добавить Docker Compose для PostgreSQL

Создать `backend/compose.yaml` со службой PostgreSQL:

- образ `postgres:16`;
- БД `pipeline_crm`;
- пользователь `pipeline_user`;
- пароль читается из `.env`;
- порт `5432:5432`;
- named volume для данных;
- healthcheck с `pg_isready`.

Создать `backend/.env.example` без настоящих секретов.

Готово, если `docker compose up -d` успешно поднимает БД.

### S1-03. Настроить конфигурацию приложения

Создать `application.yml` и `application-dev.yml`.

Настроить:

- PostgreSQL через переменные окружения;
- Flyway;
- `ddl-auto: validate`;
- timezone Hibernate = UTC;
- SQL-логи только в `dev` профиле;
- порт `8080`.

Готово, если Flyway применяет миграции при старте, а Hibernate не пытается создавать таблицы.

### S1-04. Создать начальную миграцию схемы

Создать `V1__initial_schema.sql`.

Создать таблицы:

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

Требования к таблицам:

- `users`: `id`, `username` unique, `password_hash`, `full_name`, `avatar_color`, `role` (`ADMIN`/`CURATOR`), `status` (`ACTIVE`/`BLOCKED`), `last_login_at`, timestamps, `deleted_at`.
- `pipeline_stages`: `id`, `name`, `position` unique, `norm_days` nullable, `is_final`, `is_active`, timestamps.
- `cohorts`: `id`, `name` unique, `start_date` unique, timestamps, `archived_at`.
- `leads`: `id`, `name`, `telegram_username`, `price_description`, `postpay_percent`, `next_ping_at`, `status` (`ACTIVE`/`ARCHIVED`/`CONVERTED`), `assigned_curator_id`, `created_by_id`, `converted_student_id` unique nullable, `archived_at`, `archived_by_id`, timestamps, `deleted_at`.
- `lead_notes`: `id`, `lead_id`, `text`, `position`, timestamps.
- `students`: `id`, `full_name`, `source_lead_id` unique nullable, `current_stage_id`, `curator_id`, `cohort_id`, `stage_entered_at`, `started_at`, `is_paused`, `paused_at`, `postpay_percent`, `created_by_id`, timestamps, `deleted_at`.
- `student_notes`: `id`, `student_id`, `text`, `position`, timestamps.
- `student_stage_history`: `id`, `student_id`, `stage_id`, `entered_at`, `exited_at` nullable, `changed_by_id`, `created_at`.
- `student_curator_history`: `id`, `student_id`, `from_curator_id` nullable, `to_curator_id`, `changed_by_id`, `changed_at`.
- `student_comments`: `id`, `student_id`, `author_id`, `text`, timestamps, `deleted_at`.
- `audit_log`: `id`, `actor_id` nullable, `entity_type`, `entity_id`, `action`, `before_data jsonb`, `after_data jsonb`, `created_at`.

Добавить FK, `CHECK`-ограничения и индексы на все FK. Добавить индексы на `users.username`, `leads.status`, `leads.assigned_curator_id`, `students.curator_id`, `students.current_stage_id`, `students.cohort_id`.

Готово, если миграция применяется на пустой БД и повторный запуск приложения проходит.

### S1-05. Реализовать JPA Entity, enum и repository

Создать Entity, enum и Spring Data repository для всех таблиц из S1-04.

Требования:

- Entity соответствуют миграции без расхождений.
- Все ID имеют тип `UUID`.
- `createdAt` и `updatedAt` заполняются автоматически.
- В `User` есть только `passwordHash`, открытого пароля нет.
- Не использовать `CascadeType.REMOVE` для бизнес-данных.

Готово, если Spring context стартует с `ddl-auto=validate`, а repository-тест сохраняет и читает базовые сущности.

### S1-06. Добавить seed-данные для разработки

Создать `V2__seed_development_data.sql`.

Добавить:

- одного активного ADMIN с заранее подготовленным BCrypt-хешем пароля;
- десять этапов из `pipeline-crm-prototype.html`;
- трех тестовых CURATOR;
- шесть когорт.

Готово, если чистая БД после запуска содержит эти записи и ни одна миграция не содержит пароль открытым текстом.

### S1-07. Реализовать CRUD этапов

Создать API:

```text
GET    /api/v1/pipeline-stages
POST   /api/v1/pipeline-stages
PATCH  /api/v1/pipeline-stages/{id}
PUT    /api/v1/pipeline-stages/order
POST   /api/v1/pipeline-stages/{id}/archive
```

Правила:

- `normDays` положительный или `null`.
- У финального этапа `normDays = null`.
- Архивированный этап нельзя назначить новому ученику.
- Новый порядок этапов передается списком ID без повторов.
- Пока security не добавлен, но контроллеры, сервисы и DTO должны быть готовы к аннотациям ролей.

Готово, если API покрыто controller-тестами; валидация возвращает `400`, отсутствующая сущность - `404`.

### S1-08. Реализовать CRUD когорт

Создать API:

```text
GET   /api/v1/cohorts
POST  /api/v1/cohorts
PATCH /api/v1/cohorts/{id}
POST  /api/v1/cohorts/{id}/archive
```

DTO когорты: `id`, `name`, `startDate`, `archivedAt`.

Готово, если когорту можно создать, изменить, архивировать и получить списком; уникальность имени и даты проверяется сервером.

## Спринт 2. Авторизация и роли

### S2-01. Подключить Spring Security

Добавить Spring Security.

Настроить:

- `/api/v1/health` публичный;
- остальные `/api/v1/**` требуют аутентификации;
- stateless security без HTTP-сессии;
- CSRF выключен для REST API;
- CORS разрешает только origin из переменной окружения frontend;
- единый JSON-ответ для `401` и `403`.

Готово, если защищенный маршрут без токена отвечает `401`.

### S2-02. Реализовать JWT-аутентификацию

Создать API:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

Правила:

- `login` принимает `username` и `password`.
- Access token действует 15 минут и возвращается в JSON.
- Refresh token действует 7 дней и хранится в `HttpOnly`, `SameSite=Lax` cookie.
- Флаг `Secure` cookie конфигурируется через свойства приложения для dev/prod.
- Заблокированный пользователь при логине получает `403`.
- Проверять пароль через `BCryptPasswordEncoder`.
- `auth/me` возвращает `id`, `username`, `fullName`, `role`, `avatarColor`.

Готово, если login, refresh и logout работают; неверные данные дают `401`; блокировка дает `403`.

### S2-03. Реализовать управление пользователями

Создать API только для ADMIN:

```text
GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
POST   /api/v1/users/{id}/block
POST   /api/v1/users/{id}/unblock
POST   /api/v1/users/{id}/reset-password
DELETE /api/v1/users/{id}
```

Правила:

- Создавать пользователей с ролями `ADMIN` или `CURATOR`.
- Пароль принимается только при создании и сбросе; API никогда не возвращает хеш или пароль.
- Нельзя заблокировать или удалить последнего активного ADMIN.
- Удаление пользователя - soft delete.
- Куратора с активными учениками нельзя удалить до их переназначения.

Готово, если CURATOR получает `403` на всех маршрутах, а ответы не содержат данных пароля.

### S2-04. Реализовать CRUD лидов с RBAC

Создать API:

```text
GET  /api/v1/leads
POST /api/v1/leads
GET  /api/v1/leads/{id}
PATCH /api/v1/leads/{id}
POST /api/v1/leads/{id}/archive
POST /api/v1/leads/{id}/restore
POST /api/v1/leads/{id}/postpone-ping
```

Поддержать фильтры: `status`, `search`, `assignedCuratorId`, `pingFrom`, `pingTo`.

Правила:

- ADMIN работает со всеми лидами.
- CURATOR работает только с лидами, у которых `assignedCuratorId` равен ID текущего пользователя.
- Новый лид CURATOR автоматически назначается на него.
- CURATOR не назначает лида другому пользователю.
- Архивирование и восстановление не удаляют запись.
- Перенос пинга принимает дату не раньше текущего дня.
- Заметки лида передавать как массив `{ text, position }`.
- При попытке CURATOR получить чужой лид отвечать `404`, не `403`.

Готово, если фильтрация и поиск работают только в разрешенном наборе данных.

### S2-05. Реализовать конвертацию лида в ученика

Создать `POST /api/v1/leads/{id}/convert`.

Тело запроса:

```json
{
  "curatorId": "uuid",
  "cohortId": "uuid",
  "postpayPercent": 70,
  "startedAt": "2026-09-07T12:00:00Z"
}
```

Правила:

- Операция выполняется в одной транзакции.
- Создать ученика на первом активном этапе.
- Создать открытую запись `student_stage_history` с `exited_at = null`.
- Скопировать `lead_notes` в `student_notes`.
- Обновить лид: `status = CONVERTED`, заполнить `convertedStudentId`.
- Повторная конвертация возвращает `409`.
- ADMIN передает `curatorId`.
- CURATOR может конвертировать только свой лид и назначает только себя.

Готово, если ошибка операции не оставляет частичных записей в БД.

### S2-06. Реализовать учеников и Kanban-операции

Создать API:

```text
GET  /api/v1/students
POST /api/v1/students
GET  /api/v1/students/{id}
PATCH /api/v1/students/{id}
POST /api/v1/students/{id}/move-stage
POST /api/v1/students/{id}/pause
POST /api/v1/students/{id}/resume
POST /api/v1/students/{id}/assign-curator
GET  /api/v1/students/{id}/history
```

Фильтры списка: `stageId`, `curatorId`, `cohortId`, `onlyOverdue`, `search`.

Правила:

- ADMIN работает со всеми учениками.
- CURATOR работает только со своими учениками.
- CURATOR не может менять `curatorId`, `cohortId` и создавать ученика для другого куратора.
- Смена этапа закрывает открытую запись истории и создает новую, обновляет `currentStageId` и `stageEnteredAt`.
- Нельзя переместить ученика на архивный этап.
- Пауза не изменяет историю этапов.
- Сменить куратора может только ADMIN.
- Статус здоровья вычислять в сервисе, не сохранять в БД.

Логика здоровья:

- `paused`, если ученик на паузе;
- `green`, если прошло меньше 70% нормы;
- `yellow`, если прошло от 70% до 100% нормы включительно;
- `red`, если прошло больше 100% нормы;
- этап без нормы: `green`, если ученик не на паузе.

Готово, если CURATOR не может читать или менять чужого ученика, а `onlyOverdue=true` исключает учеников на паузе.

### S2-07. Реализовать комментарии учеников

Создать API:

```text
GET    /api/v1/students/{id}/comments
POST   /api/v1/students/{id}/comments
PATCH  /api/v1/students/{id}/comments/{commentId}
DELETE /api/v1/students/{id}/comments/{commentId}
```

Правила:

- ADMIN управляет комментариями всех учеников.
- CURATOR работает только с комментариями своих учеников.
- CURATOR редактирует и удаляет только комментарии, автором которых является он.
- Удаление мягкое.

Готово, если комментарий содержит автора и дату, а неавторизованное изменение возвращает `403`.

### S2-08. Реализовать статистику с RBAC

Создать API:

```text
GET /api/v1/stats/overview
GET /api/v1/stats/stages
GET /api/v1/stats/curators
GET /api/v1/stats/overdue-students
GET /api/v1/stats/cohorts
GET /api/v1/stats/cohorts/{id}
```

Правила:

- Для ADMIN статистика строится по всем ученикам.
- Для CURATOR статистика строится только по его ученикам.
- CURATOR не получает таблицу других кураторов.
- Плановый этап рассчитывается по сумме нормативов предыдущих этапов.
- Когорта - отдельная редактируемая метка ученика; план конкретного ученика считается от его `startedAt`, а не от первого дня когорты.

Готово, если API CURATOR не раскрывает количество, имена или показатели других кураторов.

### S2-09. Реализовать аудит критичных действий

Записывать в `audit_log`:

- создание, блокировку, разблокировку, удаление пользователя;
- изменение и архивирование этапов и когорт;
- архивирование и восстановление лида;
- конвертацию лида;
- смену этапа, куратора, паузу и возобновление ученика;
- soft delete критичных сущностей.

В записи сохранять `actorId`, `entityType`, `entityId`, `action`, `beforeData`, `afterData`, `createdAt`.

Готово, если все перечисленные операции создают запись без паролей, хешей или токенов.

## Спринт 3. Frontend-компоненты

### S3-01. Создать React/Vite/TypeScript проект

Создать проект в папке `frontend`.

Подключить React, TypeScript, Vite, Tailwind CSS, Axios, React Router и ESLint.

Создать структуру:

```text
src/
  api/
  auth/
  components/
  hooks/
  pages/
  types/
  utils/
  styles/
```

Готово, если `npm run dev` и `npm run build` проходят, а Tailwind применяется.

### S3-02. Настроить Axios client

Создать `src/api/client.ts`.

Требования:

- `baseURL` берется из `VITE_API_BASE_URL`.
- Access token хранится только в памяти приложения.
- Request interceptor добавляет `Authorization: Bearer <token>`.
- На первом `401` выполнить `POST /auth/refresh`, затем повторить исходный запрос.
- При неуспешном refresh очистить авторизацию и перенаправить на `/login`.
- Не допустить бесконечный refresh loop.
- Запросы refresh и logout используют `withCredentials: true`.

Готово, если сессия восстанавливается после обновления страницы через refresh cookie.

### S3-03. Реализовать AuthProvider и маршруты

Создать `AuthProvider`, `useAuth`, `ProtectedRoute`, `RoleRoute`.

Создать маршруты:

```text
/login
/leads
/students
/statistics
/admin
```

Правила:

- Неавторизованный пользователь перенаправляется на `/login`.
- CURATOR не может открыть `/admin`.
- После успешного логина перенаправлять на `/leads`.
- Logout очищает пользователя и access token.

Готово, если маршруты ограничены по роли.

### S3-04. Создать страницу логина

Создать `LoginPage` с полями login/password, кнопкой входа, состоянием загрузки и понятным сообщением об ошибке.

После успешного входа:

1. Сохранить access token в AuthProvider.
2. Запросить `/auth/me`.
3. Сохранить пользователя в AuthProvider.
4. Перейти на `/leads`.

Готово, если неверный пароль отображает ошибку, повторная отправка формы блокируется, пароль не попадает в console.

### S3-05. Создать общий layout и навигацию

Создать `AppLayout`, верхнюю панель или sidebar, отображение профиля и кнопку logout.

Разделы: Лиды, Ученики, Статистика, Админка только для ADMIN.

Готово, если активный раздел выделен, а навигация не ломается на мобильной ширине.

### S3-06. Создать типы и API-модули

Создать:

```text
api/auth.ts
api/leads.ts
api/students.ts
api/stages.ts
api/cohorts.ts
api/statistics.ts
api/users.ts
```

Создать TypeScript-типы: `User`, `Role`, `Lead`, `LeadStatus`, `Student`, `StudentHealth`, `PipelineStage`, `Cohort`, `StudentComment`, DTO фильтров и операций.

Готово, если компоненты не используют `any`, а URL и DTO не дублируются в UI.

### S3-07. Реализовать страницу лидов

Создать `LeadsPage` и компоненты `LeadFilters`, `LeadList`, `LeadCard`, `LeadFormModal`, `ConvertLeadModal`.

Функции:

- поиск по имени и Telegram username;
- переключение активных и архивных;
- создание и редактирование;
- изменение даты пинга;
- архивирование и восстановление;
- конвертация лида в ученика.

RBAC интерфейса:

- CURATOR не видит выбор другого куратора;
- ADMIN может назначать куратора;
- серверные ошибки прав нужно корректно отображать.

Готово, если страница использует API, а не демо-массивы; конвертированный лид исчезает из активного списка.

### S3-08. Реализовать Kanban учеников

Создать `StudentsPage` и компоненты `StudentFilters`, `KanbanBoard`, `KanbanColumn`, `StudentCard`, `StudentDetailsModal`, `StudentHistory`, `StudentComments`.

Функции:

- загрузить учеников и активные этапы через API;
- группировать карточки по этапам;
- искать по имени;
- фильтровать по куратору для ADMIN;
- фильтровать просроченных;
- отображать health status;
- ставить на паузу и снимать с паузы;
- добавлять комментарии;
- реализовать drag-and-drop этапов;
- показывать переназначение куратора только ADMIN.

Правило drag-and-drop: не менять карточку окончательно в UI до успешного ответа `POST /students/{id}/move-stage`; при ошибке обновить данные с сервера.

Готово, если CURATOR видит только свои карточки и Kanban имеет горизонтальную прокрутку на узком экране.

### S3-09. Реализовать страницу статистики

Создать `StatisticsPage` и компоненты `OverviewStats`, `StageStatisticsTable`, `CuratorStatisticsTable`, `OverdueStudentsTable`, `CohortFunnelTable`, `CohortDetailsModal`.

Функции:

- health-сводка;
- таблица этапов;
- таблица просроченных;
- таблица кураторов для ADMIN;
- когортная воронка;
- детали когорты.

Готово, если frontend не дублирует расчет бизнес-метрик, а CURATOR не видит данные других кураторов.

### S3-10. Реализовать админку

Создать `AdminPage` только для ADMIN и компоненты `StageSettings`, `CohortSettings`, `UserManagement`, `CreateUserModal`, `ResetPasswordModal`.

Функции:

- редактировать имя и нормативы этапов;
- менять порядок этапов;
- архивировать этапы;
- создавать, изменять, архивировать когорты;
- создавать, блокировать, разблокировать и soft delete пользователей;
- запускать сброс пароля.

Правила:

- опасные операции требуют подтверждения в modal;
- открытый пароль нельзя показывать после создания или сброса;
- CURATOR не должен иметь доступ к странице по меню или прямому URL.

Готово, если все действия вызывают соответствующий API.

### S3-11. Добавить общие состояния UI

Создать компоненты `Loader`, `EmptyState`, `ErrorState`, `ConfirmDialog`, `Toast`. Добавить `Pagination`, если списочные API возвращают страницы.

Требования:

- У каждого сетевого экрана есть loading, error и empty состояния.
- Архивирование, удаление, блокировка и конвертация требуют подтверждения.
- Ошибки `401`, `403`, `404`, `409`, `422` выводятся понятным текстом без stack trace.

Готово, если приложение корректно ведет себя при недоступном backend.

### S3-12. Провести сквозную интеграционную проверку

Проверить вручную и, где возможно, автоматизировать следующие сценарии:

1. ADMIN входит, создает куратора, этап и когорту.
2. CURATOR входит, создает лида, переносит пинг и конвертирует его.
3. CURATOR передвигает своего ученика, ставит его на паузу и добавляет комментарий.
4. ADMIN переназначает ученика другому куратору.
5. CURATOR не получает чужого лида или ученика через прямой API-вызов.
6. ADMIN видит общую статистику, CURATOR - только свою.
7. Заблокированный пользователь не может войти.
8. После перезагрузки страницы сессия восстанавливается через refresh token.

Готово, если все сценарии проходят на локальном PostgreSQL, `backend ./mvnw test` и `frontend npm run build` завершаются успешно.
