# Технический план: React-фронтенд Pipeline CRM

Статус: реализуется в этой сессии. Источники истины: `pipeline-crm-prototype.html` (UX/бизнес-логика),
`DEVELOPMENT_BACKLOG.md` (Спринт 3, S3-01..S3-12), реализованный backend в `backend/` (контракты DTO ниже
сняты непосредственно с исходников контроллеров/DTO после бэкенд-фиксов этой сессии).

## 1. Стек и ограничения

Строго по backlog: **React 18 + TypeScript + Vite + Tailwind CSS + Axios + React Router**. Без Redux/React
Query/UI-кит библиотек — состояние через hooks (`useState`/`useEffect`/небольшие кастомные хуки на
страницу), это соответствует масштабу ("максимум 10 одновременных пользователей") и духу backlog.

Папка: `frontend/` рядом с `backend/`.

```
frontend/
  src/
    api/        client.ts, auth.ts, leads.ts, students.ts, stages.ts, cohorts.ts, statistics.ts, users.ts
    auth/       AuthProvider.tsx, useAuth.ts, ProtectedRoute.tsx, RoleRoute.tsx
    components/ Loader, EmptyState, ErrorState, ConfirmDialog, Toast, Avatar, HealthBadge, PingBadge, TimeBadge
    hooks/      useLeads, useStudents, useStages, useCohorts, useUsers, useStatistics, useToast
    pages/      LoginPage, LeadsPage, StudentsPage, StatisticsPage, AdminPage, layout/AppLayout
    pages/leads/    LeadFilters, LeadList, LeadCard, LeadFormModal, ConvertLeadModal
    pages/students/ KanbanBoard, KanbanColumn, StudentCard, StudentDetailsModal, StudentHistory, StudentComments, StudentFilters
    pages/stats/    OverviewStats, StageStatisticsTable, CuratorStatisticsTable, OverdueStudentsTable, CohortFunnelTable, CohortDetailsModal
    pages/admin/    StageSettings, CohortSettings, UserManagement, CreateUserModal, ResetPasswordModal
    types/      index.ts (mirrors backend DTOs 1:1, see §3)
    utils/      dates.ts, health.ts, plural.ts
    styles/     tokens.css (CSS vars ported 1:1 from the prototype's `.crm-root` palette), index.css (Tailwind layers)
```

## 2. Визуальный язык (портируется из прототипа, не переизобретается)

Цвета и типографика берутся из `.crm-root` в `pipeline-crm-prototype.html` дословно (CSS custom
properties `--bg #F5F6F8`, `--surface #fff`, `--ink-900/600/400`, `--accent #2F5EFF` + `--accent-soft`,
`--warn #E0432B`, `--amber #C97A11`, `--success #17875A`, `--pause #7A7F8A`, каждый с `-soft` вариантом).
Шрифты — те же Google Fonts: Space Grotesk (заголовки/имена), Inter (текст), IBM Plex Mono (числа/бейджи).
Tailwind настраивается через `theme.extend.colors` со ссылкой на CSS-переменные (`bg: 'var(--bg)'` и т.д.),
чтобы использовать `bg-surface`, `text-ink-600`, `border-border` и т.п., сохраняя палитру в одном месте.

Компоненты-бейджи копируют семантику 1:1:
- `HealthBadge` (🟢/🟡/🔴/⚪ = green/yellow/red/paused) — цвет из `health` поля StudentDto/статистики.
- `PingBadge` (overdue/today/upcoming) — как `pingStatus()` в прототипе.
- `TimeBadge` на карточке ученика (ok/warn/critical/paused) — как `timeTier()`.
- Kanban-колонки: "spine" прогресс-полоска, число учеников, норма дней, `contenteditable`-заголовок
  только для ADMIN (PATCH `/pipeline-stages/{id}` при blur).

## 3. Контракты API (сняты с backend после бэкенд-фиксов этой сессии)

Базовый путь `/api/v1`. Access token — только в памяти (AuthProvider state), refresh — HttpOnly cookie,
все auth/refresh/logout запросы идут с `withCredentials: true`.

### Auth
- `POST /auth/login {username,password}` → `{accessToken, user: AuthUserDto}` ; 401 неверный пароль, 403 заблокирован
- `POST /auth/refresh` (cookie) → тот же `AuthResponse`
- `POST /auth/logout` (cookie) → 204
- `GET /auth/me` → `AuthUserDto {id, username, fullName, role: 'ADMIN'|'CURATOR', avatarColor}`
- `PATCH /auth/me/password {currentPassword, newPassword}` → 204

### Users (ADMIN only, 403 иначе)
`GET/POST /users`, `GET/PATCH/DELETE /users/{id}`, `POST /users/{id}/block|unblock|reset-password`.
`UserDto {id, username, fullName, avatarColor, role, status:'ACTIVE'|'BLOCKED', lastLoginAt, createdAt}`.
Создание: `{username, password, fullName, avatarColor?, role}`. Пароль никогда не возвращается.
409 при удалении/блокировке последнего активного ADMIN или удалении куратора с активными учениками.

### Pipeline stages
`GET /pipeline-stages` (все аутентифицированные), `POST/PATCH/PUT order/POST archive` — ADMIN only.
`StageDto {id, name, position, normDays: number|null, isFinal, isActive}`. Список уже отсортирован и
содержит только активные этапы (`findAllActiveOrdered`).

### Cohorts
`GET /cohorts` (auth), мутации — ADMIN only. `CohortDto {id, name, startDate: 'YYYY-MM-DD', archivedAt}`.

### Leads
`GET /leads?status&search&assignedCuratorId&pingFrom&pingTo` (curator видит только свои — сервер
фильтрует сам), `POST/GET/PATCH /leads/{id}`, `POST /{id}/archive|restore|postpone-ping|convert`.
`LeadDto {id, name, telegramUsername, priceDescription, postpayPercent, nextPingAt, status:
'ACTIVE'|'ARCHIVED'|'CONVERTED', assignedCuratorId, createdById, convertedStudentId, archivedAt,
notes:[{id,text,position}]}`. `postpone-ping` 409 если дата раньше начала сегодняшнего дня (UTC).
`convert {curatorId?, cohortId?, postpayPercent?, startedAt?}` — curatorId обязателен только когда
вызывает ADMIN (400 `curatorId is required` иначе); для CURATOR поле не показывать в форме вовсе.

### Students
`GET /students?stageId&curatorId&cohortId&onlyOverdue&search`, `POST/GET/PATCH /students/{id}`,
`POST /{id}/move-stage {stageId}`, `/pause`, `/resume`, `/assign-curator {curatorId}` (ADMIN only),
`GET /{id}/history` → `{stages:[{stageId,enteredAt,exitedAt,changedById}], curators:[{fromCuratorId,
toCuratorId,changedAt}]}`, `GET/POST /{id}/comments`, `PATCH/DELETE /{id}/comments/{commentId}`.
`StudentDto {id, fullName, sourceLeadId, currentStageId, curatorId, cohortId, stageEnteredAt, startedAt,
isPaused, pausedAt, postpayPercent, createdById, health: 'green'|'yellow'|'red'|'paused', notes:[...]}`.
`CommentDto {id, studentId, authorId, text, createdAt, updatedAt}` — CURATOR редактирует/удаляет
только свои (403 иначе), curatorId/cohortId в PATCH — 403 если меняет не ADMIN.
Kanban drag&drop правило: карточку не переносить оптимистично — ждать 200 от move-stage, при ошибке
`GET /students` заново.

### Statistics (CURATOR получает то же с автоматически урезанными данными от сервера)
- `GET /stats/overview` → `StatsOverview {total, green, yellow, red, paused}`
- `GET /stats/stages` → `StageStats[] {stageId, name, normDays, onStage, stuck}`
- `GET /stats/curators` → `CuratorStats[] {curatorId, name, avatarColor, count, stuck, avgPct}`
  (для CURATOR всегда массив из одного элемента — своего)
- `GET /stats/overdue-students` → `OverdueStudent[] {studentId, name, stageId, stageName, normDays,
  curatorId, curatorName, daysOnStage}`
- `GET /stats/cohorts` / `GET /stats/cohorts/{id}` → `CohortStats {cohortId, name, startDate, total,
  reachedCounts: number[] (по позиции этапа), plannedStagePosition, onTrackCount, behindCount}`

### Ошибки
Все контроллеры отдают `{timestamp, status, message, errors?}` (validation — `errors` как
`{field: message}`). 401/403 из security layer — `{status, message}` (без `timestamp/errors`). Axios
response interceptor нормализует оба варианта в единый `ApiError {status, message, errors?}`.

## 4. Auth-поток и RBAC на фронте (S3-02/03/04)

- `client.ts`: `baseURL = import.meta.env.VITE_API_BASE_URL`, request interceptor добавляет
  `Authorization: Bearer <token>` из модуля-синглтона (не localStorage — access token только в памяти).
  Response interceptor: на первый 401 (кроме самого `/auth/refresh`) — вызвать `POST /auth/refresh`
  (`withCredentials`), при успехе повторить исходный запрос с новым токеном; при неуспехе —
  `clearAuth()` + редирект на `/login`. Флаг `isRefreshing` + очередь ожидающих запросов предотвращает
  parallel refresh storm/loop.
- `AuthProvider`: держит `{user, accessToken, status}`, на маунте молча пробует `/auth/refresh` (сессия
  восстанавливается по cookie после reload), не блокируя рендер дольше одного лоадера.
- `ProtectedRoute`: нет user → `/login`. `RoleRoute(role='ADMIN')`: не ADMIN → редирект на `/leads`
  (Админку не открыть даже прямым URL).
- Роуты: `/login`, `/leads`, `/students`, `/statistics`, `/admin` (ADMIN only). После логина → `/leads`.
  Logout — очистка user/token + `POST /auth/logout`.
- UI-RBAC (сервер всё равно проверяет заново, это только UX):
  - CURATOR не видит выбор куратора при создании лида/конвертации, не видит переназначение куратора
    в карточке ученика (select disabled), не видит пункт меню "Админка".
  - 403 с сервера на любое действие → toast с понятным текстом, без деталей стека.

## 5. Страницы и приёмка (S3-05..S3-11)

1. **AppLayout** — топбар (`Пайплайн`, вкладки по ролям как `canSeeTab` в прототипе — но здесь у CURATOR
   всегда есть Лиды/Ученики/Статистика, т.к. видимость по seeLeads/seeStats была фронтовой имитацией в
   прототипе и в backend такого флага на пользователе нет; см. §7 "Отличия от прототипа"), профиль +
   logout, кнопка "+ Лид" на вкладке Лидов.
2. **LoginPage** — username/password, loading state, "неверный логин/пароль" на 401/403 без деталей,
   повторная отправка блокируется на время запроса.
3. **LeadsPage** — поиск (имя+telegram), toggle активные/архив, `LeadCard` (bullets = notes, price =
   priceDescription, ping badge), действия: конвертировать → `ConvertLeadModal` (выбор куратора только
   ADMIN, когорта, % постоплаты, дата старта) → создаёт ученика и убирает лид из активного списка;
   +2 дня → `postpone-ping`; выкинуть/восстановить → archive/restore; `LeadFormModal` для создания/
   редактирования с динамическим списком заметок (`notes[]`).
4. **StudentsPage** — `KanbanBoard`: колонки по `stages` (норма/финальный шаг подпись), карточки
   отсортированы как в прототипе (paused последними, red по убыванию просрочки, затем по имени),
   HTML5 drag&drop с ожиданием ответа сервера; `StudentFilters` (чипы кураторов только видимым
   ADMIN — CURATOR видит только свои карточки, сервер и не отдаст чужие; toggle "только просрочившие" →
   `onlyOverdue=true`; поиск по имени). `StudentDetailsModal`: постоплата/план завершения, куратор
   (select disabled для CURATOR), пауза/снятие с паузы, заметки (read-only, из notes[]), комментарии
   (добавление всем, редактирование/удаление — автору или ADMIN), `StudentHistory` (объединённая лента
   `stages`+`curators`, отсортированная по времени, текущий этап — "сейчас").
5. **StatisticsPage** — вкладки "Основная"/"Дополнительная" как в прототипе: `OverviewStats` (5 плашек),
   `StageStatisticsTable`, `CuratorStatisticsTable` (для CURATOR — одна строка, без кросс-данных),
   `OverdueStudentsTable`; доп. вкладка — `CohortFunnelTable` (проценты доходимости по этапам,
   выделение `plannedStagePosition` рамкой) + клик по строке → `CohortDetailsModal` (онTrack/behind %,
   список учеников когорты).
6. **AdminPage** (ADMIN only) — `StageSettings` (редактирование normDays инлайн, drag-переупорядочивание
   → `PUT /pipeline-stages/order`, архивация), `CohortSettings` (создать/редактировать/архивировать),
   `UserManagement` (карточки кураторов+админов, block/unblock, reset-password через модалку с
   одноразовым показом нового пароля, soft delete с подтверждением, disabled для последнего ADMIN/
   куратора с учениками — сервер вернёт 409, показать как toast).
7. **Общие состояния (S3-11)** — `Loader`, `EmptyState`, `ErrorState`, `ConfirmDialog` (архивация,
   удаление, блокировка, конвертация — всегда через confirm), `Toast` (успех/ошибка), каждый
   сетевой экран покрывает loading/error/empty.

## 6. Порядок реализации (в этой сессии, последовательно)

1. Скаффолдинг Vite+React+TS в `frontend/`, Tailwind, ESLint, базовая структура папок.
2. `types/index.ts` — все DTO из §3.
3. `api/client.ts` + auth-модуль + `AuthProvider`/`ProtectedRoute`/`RoleRoute`.
4. `styles/tokens.css` (палитра прототипа) + Tailwind config, `AppLayout` + роутинг + `LoginPage`.
5. Общие компоненты (Loader/EmptyState/ErrorState/ConfirmDialog/Toast/Avatar/бейджи) + утилиты
   (dates/health/plural).
6. Лиды: api-модуль, хук, страница, модалки.
7. Ученики: api-модуль, хук, Kanban + модалка (история/комментарии/пауза/переназначение).
8. Статистика: api-модуль, хук, таблицы + модалка когорты.
9. Админка: этапы/когорты/пользователи + модалки.
10. `npm run build` (типы и сборка проходят без ошибок).
11. E2E-проверка против реально запущенного backend (docker compose + `./mvnw spring-boot:run`) —
    логин admin и curator, обзор всех вкладок, минимум одно реальное действие с бэкендом
    (создание лида, конвертация, drag-and-drop, проверка RBAC), фиксация найденных багов.

## 7. Отличия от прототипа (осознанные, т.к. прототип — не источник правды для авторизации)

- Роли/права не редактируются per-curator флагами (canMoveOthers/canReassign/seeLeads/seeStats) — в
  backend их нет, RBAC полностью серверный и двухролевой (ADMIN/CURATOR). Соответствующий UI из
  прототипа (тумблеры прав в Админке) не переносится.
- Пароли кураторов не хранятся в открытом виде и не показываются в Админке — только сброс пароля с
  одноразовым отображением нового значения, как того требует backlog.
- "+ Лид" — полноценная форма (`LeadFormModal`), а не `prompt()`.
