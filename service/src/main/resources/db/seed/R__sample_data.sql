-- Учебные данные для локальной разработки. Накатываются только при database.seed=true
-- (docker compose), в тестах и проде не применяются.
--
-- Повторяемая миграция Flyway (R__): применяется после версионных V1, V2, …
-- и не занимает номер версии, поэтому свои миграции нумеруйте дальше — V4, V5, …
-- Вставки идемпотентны: повторный прогон ничего не дублирует.


-- Insert sample courses
INSERT INTO courses (title, description, short_description, thumbnail_url, price, original_price, duration, difficulty, is_published)
SELECT v.*
FROM (VALUES
    ('Kotlin для Android разработчиков', 'Полный курс по разработке Android приложений на Kotlin', 'Изучите основы Kotlin и Android разработки', 'https://picsum.photos/400/300', 2990.00, 3990.00, 40, 'BEGINNER', true),
    ('Advanced Android Architecture', 'Изучите современные архитектурные паттерны', 'MVVM, Clean Architecture, Dependency Injection', 'https://picsum.photos/401/300', 3990.00, 4990.00, 30, 'ADVANCED', true),
    ('Jetpack Compose для начинающих', 'Создавайте современные UI с Jetpack Compose', 'Декларативный UI, State, Navigation', 'https://picsum.photos/402/300', 2490.00, 2990.00, 25, 'INTERMEDIATE', true)
) AS v(title, description, short_description, thumbnail_url, price, original_price, duration, difficulty, is_published)
WHERE NOT EXISTS (SELECT 1 FROM courses c WHERE c.title = v.title);

-- Insert sample sprint
INSERT INTO sprints (title, description, discount_percentage, start_date, end_date, is_active)
SELECT 'Осенний марафон', 'Скидки до 50% на все курсы по Android разработке', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', true
WHERE NOT EXISTS (SELECT 1 FROM sprints WHERE title = 'Осенний марафон');

-- Link courses to sprint
INSERT INTO sprint_courses (sprint_id, course_id)
SELECT s.id, c.id
FROM sprints s, courses c
WHERE s.title = 'Осенний марафон'
  AND NOT EXISTS (SELECT 1 FROM sprint_courses sc WHERE sc.sprint_id = s.id)
LIMIT 2;

-- Insert sample roadmap
INSERT INTO roadmaps (title, description, category, is_published)
SELECT 'Android Developer Roadmap 2025', 'Путь от новичка до профессионального Android разработчика', 'Android', true
WHERE NOT EXISTS (SELECT 1 FROM roadmaps WHERE title = 'Android Developer Roadmap 2025');

-- Insert roadmap steps
INSERT INTO roadmap_steps (roadmap_id, title, description, "order", course_id)
SELECT
    r.id,
    step.title,
    step.description,
    step.order_index,
    step.course_id
FROM roadmaps r,
(VALUES
    ('Основы программирования', 'Изучите основы Kotlin и ООП', 1, (SELECT id FROM courses WHERE title = 'Kotlin для Android разработчиков' LIMIT 1)),
    ('Android Fundamentals', 'Основы разработки Android приложений', 2, (SELECT id FROM courses WHERE title = 'Jetpack Compose для начинающих' LIMIT 1)),
    ('Продвинутая разработка', 'Архитектура, тестирование, CI/CD', 3, (SELECT id FROM courses WHERE title = 'Advanced Android Architecture' LIMIT 1))
) AS step(title, description, order_index, course_id)
WHERE r.title = 'Android Developer Roadmap 2025'
  AND NOT EXISTS (SELECT 1 FROM roadmap_steps rs WHERE rs.roadmap_id = r.id AND rs.title = step.title);

-- Insert sample interview questions
INSERT INTO interview_questions (question, answer, category, difficulty, tags, is_published)
SELECT v.*
FROM (VALUES
    ('Что такое Activity в Android?', 'Activity - это компонент Android приложения, который представляет один экран с пользовательским интерфейсом.', 'Android', 'BEGINNER', '["activity", "ui", "basics"]', true),
    ('Объясните жизненный цикл Activity', 'onCreate() -> onStart() -> onResume() -> onPause() -> onStop() -> onDestroy()', 'Android', 'INTERMEDIATE', '["lifecycle", "activity", "states"]', true),
    ('Что такое ViewModel в MVVM?', 'ViewModel - это класс, который хранит и управляет UI-связанными данными, переживающими изменения конфигурации.', 'Android', 'ADVANCED', '["mvvm", "viewmodel", "architecture"]', true)
) AS v(question, answer, category, difficulty, tags, is_published)
WHERE NOT EXISTS (SELECT 1 FROM interview_questions q WHERE q.question = v.question);
