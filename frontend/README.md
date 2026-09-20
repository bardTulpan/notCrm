# Pipeline CRM — Frontend

React + TypeScript + Vite + Tailwind CSS + Axios + React Router. Реализует спецификацию из
`../FRONTEND_PLAN.md` поверх backend API из `../backend`.

## Требования

- Node.js 20+
- Запущенный backend (см. `../backend/README.md`) на `http://localhost:8080`

## Локальный запуск

```bash
npm install
npm run dev
```

Vite сам выбирает порт, начиная с `5173` (и печатает выбранный порт в консоли — если `5173` занят
другим процессом, откроется `5174` и т.д.).

**Backend должен знать актуальный порт фронтенда для CORS.** Переменная `FRONTEND_ORIGIN` в
`backend/.env` должна совпадать с портом, который реально показал Vite. Сейчас в этом окружении
`backend/.env` настроен на `http://localhost:5174`, потому что `5173` был занят посторонним
процессом при первом запуске — если у вас `5173` свободен, верните `FRONTEND_ORIGIN=http://localhost:5173`.

## Переменные окружения

`.env.development` — `VITE_API_BASE_URL` (по умолчанию `http://localhost:8080/api/v1`).

## Учётные данные для входа (seed backend)

| Логин    | Пароль     | Роль     |
|----------|------------|----------|
| admin    | admin123   | ADMIN    |
| anya.t   | curator1   | CURATOR  |
| igor.l   | curator2   | CURATOR  |
| sveta.r  | curator3   | CURATOR (заблокирован) |

## Структура

См. `../FRONTEND_PLAN.md` §1 за полным деревом папок и §3 за контрактами API. Коротко:

- `src/api/` — тонкие обёртки над Axios, по одному модулю на backend-ресурс.
- `src/auth/` — `AuthProvider` (access token только в памяти, сессия восстанавливается через
  HttpOnly refresh-cookie), `ProtectedRoute`, `RoleRoute`.
- `src/pages/` — по одной странице на вкладку (`Лиды`, `Ученики`, `Статистика`, `Админка`), с
  вложенными подпапками для их модалок/таблиц/канбана.
- `src/components/` — общие Loader/EmptyState/ErrorState/ConfirmDialog/Toast/Avatar/бейджи.
- `src/styles`/`src/index.css` — палитра и типографика 1:1 с `pipeline-crm-prototype.html`,
  подключены в Tailwind v4 через `@theme`.

## Сборка

```bash
npm run build   # tsc -b && vite build
npm run preview
```

## Проверено вручную (эта сессия)

Полный цикл против реально запущенного backend + Postgres: логин admin и curator, восстановление
сессии после reload по refresh-cookie, RBAC (curator не видит Админку и не открывает `/admin` по
прямому URL), создание и конвертация лида в ученика через реальные модалки, Kanban с
drag-and-drop (реальные HTML5 DnD события подтверждённо вызывают `POST /students/{id}/move-stage`),
модалка ученика (пауза/снятие с паузы, комментарии), статистика и админка (этапы/когорты/
пользователи) рендерятся без ошибок в консоли, кроме двух ожидаемых `403` на `/auth/refresh` до
первого логина (это штатная попытка тихого восстановления сессии без cookie).
