-- Seed development data

INSERT INTO users (id, username, password_hash, full_name, avatar_color, role, status, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-00000000a001', 'admin', '$2a$10$ZoNVnRJk3uc4wQkhiLsOAOj5U0Y0edQscat5H2h.GDaL4UaxABaNO', 'Главный Администратор', '#2F5EFF', 'ADMIN', 'ACTIVE', now(), now()),
    ('00000000-0000-0000-0000-00000000a002', 'anya.t', '$2a$10$R2c4Loc3mMyAIBa8p1e12uQ3pawDD606.Z4nCZMqyo3geu1XtKvBS', 'Аня Т.', '#17875A', 'CURATOR', 'ACTIVE', now(), now()),
    ('00000000-0000-0000-0000-00000000a003', 'igor.l', '$2a$10$8PwKrEh400N5Zva2ok2mq.Nvlt6nf5IORET.IYW5M9DY.CuQCs.aC', 'Игорь Л.', '#C97A11', 'CURATOR', 'ACTIVE', now(), now()),
    ('00000000-0000-0000-0000-00000000a004', 'sveta.r', '$2a$10$3XRAQkkBRmriHLv8dSn.XubGRtyZrnV4rC.nOxqlm1Mn0Ki7tRWCC', 'Света Р.', '#A23BAB', 'CURATOR', 'BLOCKED', now(), now());

INSERT INTO pipeline_stages (id, name, position, norm_days, is_final, is_active, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-00000000b001', 'GoPractice', 0, 30, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b002', 'Теория (проджект + техническая)', 1, 10, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b003', 'Теория продуктовая', 2, 10, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b004', 'Кейсы', 3, 14, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b005', 'Легенда и резюме', 4, 14, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b006', 'Вышел в найм', 5, 21, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b007', 'Получил 2 разбора', 6, 30, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b008', 'Оффер', 7, 14, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b009', 'Вышел на работу', 8, 60, false, true, now(), now()),
    ('00000000-0000-0000-0000-00000000b010', 'Выплатил пост оплату', 9, NULL, true, true, now(), now());

INSERT INTO cohorts (id, name, start_date, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-00000000c001', 'Апрель 2026', '2026-04-01', now(), now()),
    ('00000000-0000-0000-0000-00000000c002', 'Май 2026', '2026-05-01', now(), now()),
    ('00000000-0000-0000-0000-00000000c003', 'Июнь 2026', '2026-06-01', now(), now()),
    ('00000000-0000-0000-0000-00000000c004', 'Июль 2026', '2026-07-01', now(), now()),
    ('00000000-0000-0000-0000-00000000c005', 'Август 2026', '2026-08-01', now(), now()),
    ('00000000-0000-0000-0000-00000000c006', 'Сентябрь 2026', '2026-09-01', now(), now());
