-- Учебные данные для локальной разработки. Накатываются только при database.seed=true
-- (docker compose), в тестах и проде не применяются.


-- Insert sample courses
INSERT INTO courses (title, description, short_description, thumbnail_url, price, original_price, duration, difficulty, is_published)
VALUES 
    ('Kotlin для Android разработчиков', 'Полный курс по разработке Android приложений на Kotlin', 'Изучите основы Kotlin и Android разработки', 'https://picsum.photos/400/300', 2990.00, 3990.00, 40, 'BEGINNER', true),
    ('Advanced Android Architecture', 'Изучите современные архитектурные паттерны', 'MVVM, Clean Architecture, Dependency Injection', 'https://picsum.photos/401/300', 3990.00, 4990.00, 30, 'ADVANCED', true),
    ('Jetpack Compose для начинающих', 'Создавайте современные UI с Jetpack Compose', 'Декларативный UI, State, Navigation', 'https://picsum.photos/402/300', 2490.00, 2990.00, 25, 'INTERMEDIATE', true);

-- Insert sample sprint
INSERT INTO sprints (title, description, discount_percentage, start_date, end_date, is_active)
VALUES 
    ('Осенний марафон', 'Скидки до 50% на все курсы по Android разработке', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', true);

-- Link courses to sprint
INSERT INTO sprint_courses (sprint_id, course_id)
SELECT s.id, c.id 
FROM sprints s, courses c 
WHERE s.title = 'Осенний марафон' 
LIMIT 2;

-- Insert sample roadmap
INSERT INTO roadmaps (title, description, category, is_published)
VALUES ('Android Developer Roadmap 2025', 'Путь от новичка до профессионального Android разработчика', 'Android', true);

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
WHERE r.title = 'Android Developer Roadmap 2025';

-- Insert sample interview questions
INSERT INTO interview_questions (question, answer, category, difficulty, tags, is_published)
VALUES 
    ('Что такое Activity в Android?', 'Activity - это компонент Android приложения, который представляет один экран с пользовательским интерфейсом.', 'Android', 'BEGINNER', '["activity", "ui", "basics"]', true),
    ('Объясните жизненный цикл Activity', 'onCreate() -> onStart() -> onResume() -> onPause() -> onStop() -> onDestroy()', 'Android', 'INTERMEDIATE', '["lifecycle", "activity", "states"]', true),
    ('Что такое ViewModel в MVVM?', 'ViewModel - это класс, который хранит и управляет UI-связанными данными, переживающими изменения конфигурации.', 'Android', 'ADVANCED', '["mvvm", "viewmodel", "architecture"]', true);


