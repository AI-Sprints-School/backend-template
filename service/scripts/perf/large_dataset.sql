-- Большой набор учебных данных для главы 7 (индексы и план запроса).
--
-- На учебных данных db/seed (3 курса) EXPLAIN ANALYZE не отличает индекс от
-- последовательного чтения: таблица помещается в одну страницу. Скрипт
-- наполняет базу до объёма, где план меняется.
--
-- Запуск (база из docker compose, после старта сервиса — схему создаёт Flyway):
--   docker compose exec -T postgres psql -U postgres -d learning_platform \
--     -v courses=20000 -v lessons_per_course=10 -v users=20000 -v progress=500000 \
--     < scripts/perf/large_dataset.sql
-- Параметры можно не задавать — ниже значения по умолчанию.
-- Повторный запуск добавляет данные ещё раз; очистка — в конце файла, закомментирована.

\if :{?courses}            \else \set courses 20000            \endif
\if :{?lessons_per_course} \else \set lessons_per_course 10    \endif
\if :{?users}              \else \set users 20000              \endif
\if :{?progress}           \else \set progress 500000          \endif

\timing on
BEGIN;

INSERT INTO courses (title, description, short_description, price, duration, difficulty, is_published, lessons_count, created_at, updated_at)
SELECT
    'Курс ' || g,
    'Описание курса ' || g,
    NULL,
    (g % 50) * 100,
    60 + g % 600,
    (ARRAY['beginner', 'intermediate', 'advanced'])[1 + g % 3],
    g % 10 <> 0,                                   -- 90 % опубликованы
    :lessons_per_course,
    now() - (g || ' minutes')::interval,
    now()
FROM generate_series(1, :courses) AS g;

INSERT INTO lessons (course_id, title, description, content, duration, "order", is_published)
SELECT c.id, 'Урок ' || n, 'Описание урока ' || n, 'Текст урока ' || n, 5 + n, n, true
FROM courses c
CROSS JOIN generate_series(1, :lessons_per_course) AS n
WHERE c.title LIKE 'Курс %';

INSERT INTO users (email, password_hash, first_name, last_name)
SELECT 'perf-' || g || '@example.com', 'not-a-real-hash', 'Студент', 'Номер ' || g
FROM generate_series(1, :users) AS g;

-- Отметки прогресса: случайный студент × случайный урок, без повторов (уникальный индекс V3)
INSERT INTO user_progress (user_id, course_id, lesson_id, progress_type, is_completed, completed_at)
SELECT ids.uids[pick.u], l.course_id, l.id, 'lesson', true, pick.at
FROM (SELECT (SELECT array_agg(id) FROM users WHERE email LIKE 'perf-%') AS uids,
             (SELECT array_agg(id) FROM lessons) AS lids) ids
CROSS JOIN LATERAL (
    SELECT 1 + floor(random() * array_length(ids.uids, 1))::int AS u,
           1 + floor(random() * array_length(ids.lids, 1))::int AS l,
           now() - random() * interval '60 days' AS at,
           g
    FROM generate_series(1, :progress) AS g
) pick
JOIN lessons l ON l.id = ids.lids[pick.l]
ON CONFLICT DO NOTHING;

COMMIT;
ANALYZE;

SELECT 'courses' AS table_name, count(*) FROM courses
UNION ALL SELECT 'lessons', count(*) FROM lessons
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'user_progress', count(*) FROM user_progress;

-- Очистка набора:
-- DELETE FROM users WHERE email LIKE 'perf-%';
-- DELETE FROM courses WHERE title LIKE 'Курс %';
