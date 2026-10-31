-- Схема сервиса достижений (глава 6). Таблицы в схеме achievements; история Flyway — там же.
-- user_id и course_id — id из service-spring, внешних ключей нет: таблицы другого сервиса
-- этот сервис не видит, данные о пользователе приходят только событием.

-- Выданные достижения. Одно «курс пройден» на пару «студент + курс» — вторая страховка
-- от повтора события, первая — processed_events.
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'COURSE_COMPLETED',
    event_id UUID NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_achievements_user_course UNIQUE (user_id, course_id)
);

-- Обработанные события: id события — первичный ключ, повтор того же события не проходит вставку.
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
