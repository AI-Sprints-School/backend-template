-- Роли пользователей и уникальность отметок прогресса.
--
-- Роль хранится строкой с проверкой на уровне базы: новых ролей в курсе нет,
-- отдельная таблица ролей была бы избыточной. Роль попадает в access-токен
-- claim-ом `role`; после смены роли нужен новый вход.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'student';

ALTER TABLE users
    ADD CONSTRAINT users_role_check CHECK (role IN ('student', 'admin'));

-- Один урок засчитывается пользователю один раз, один курс — одной строкой.
-- Индексы частичные: в user_progress лежат и уроки, и курсы, и квизы.
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_progress_user_lesson
    ON user_progress (user_id, lesson_id) WHERE progress_type = 'lesson';

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_progress_user_course
    ON user_progress (user_id, course_id) WHERE progress_type = 'course';

-- Автор курса: черновик (is_published = false) видят только автор и администратор.
-- Курс переживает удаление автора — ссылка обнуляется, черновик остаётся администратору.
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS author_id UUID REFERENCES users(id) ON DELETE SET NULL;
