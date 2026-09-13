# Схема базы данных Learning Platform

## Обзор

База данных построена на PostgreSQL 15+ и содержит все необходимые таблицы для функционирования образовательной платформы.

## Диаграмма связей

```
users
  ├─── email_verification_tokens
  ├─── password_reset_tokens
  ├─── refresh_tokens
  ├─── user_enrollments ─── courses
  ├─── user_progress ─── courses
  ├─── completed_lessons ─── lessons
  ├─── test_sessions ─── tests
  └─── interview_questions

courses
  ├─── lessons ─── lesson_contents
  ├─── tests ─── questions ─── answers
  ├─── user_enrollments
  ├─── sprint_courses ─── sprints
  └─── roadmap_stage_courses ─── roadmap_stages ─── roadmaps
```

## Таблицы

### 1. Аутентификация и пользователи

#### users
Основная таблица пользователей системы.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | Хеш пароля (BCrypt), NULL для OAuth |
| first_name | VARCHAR(100) | Имя |
| last_name | VARCHAR(100) | Фамилия |
| profile_photo | VARCHAR(500) | URL фотографии профиля |
| auth_provider | VARCHAR(20) | LOCAL, GOOGLE |
| email_verified | BOOLEAN | Подтвержден ли email |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |
| updated_at | TIMESTAMP WITH TIME ZONE | Дата обновления |

**Индексы:**
- `idx_users_email` на `email`
- `idx_users_auth_provider` на `auth_provider`

#### email_verification_tokens
Токены для подтверждения email.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| token | VARCHAR(255) | UNIQUE токен |
| expires_at | TIMESTAMP WITH TIME ZONE | Срок действия |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

#### password_reset_tokens
Токены для сброса пароля.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| token | VARCHAR(255) | UNIQUE токен |
| expires_at | TIMESTAMP WITH TIME ZONE | Срок действия |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

#### refresh_tokens
Refresh токены для JWT.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| token | VARCHAR(500) | UNIQUE токен |
| expires_at | TIMESTAMP WITH TIME ZONE | Срок действия |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

---

### 2. Курсы и уроки

#### courses
Каталог курсов.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| title | VARCHAR(255) | Название курса |
| description | TEXT | Описание |
| cover_image | VARCHAR(500) | URL обложки |
| category | VARCHAR(100) | Категория |
| difficulty | VARCHAR(20) | BEGINNER, INTERMEDIATE, ADVANCED |
| duration | INT | Продолжительность в часах |
| lessons_count | INT | Количество уроков |
| rating | DECIMAL(3,2) | Рейтинг (0-5) |
| students_count | INT | Количество студентов |
| price | DECIMAL(10,2) | Цена |
| is_premium | BOOLEAN | Премиум курс |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |
| updated_at | TIMESTAMP WITH TIME ZONE | Дата обновления |

**Индексы:**
- `idx_courses_category` на `category`
- `idx_courses_difficulty` на `difficulty`
- `idx_courses_is_premium` на `is_premium`
- `idx_courses_rating` на `rating DESC`

**Триггеры:**
- `update_courses_updated_at` - автообновление `updated_at`

#### lessons
Уроки курсов.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| course_id | UUID | FOREIGN KEY → courses(id) |
| title | VARCHAR(255) | Название урока |
| description | TEXT | Описание |
| order_index | INT | Порядок в курсе |
| duration | INT | Продолжительность в минутах |
| video_url | VARCHAR(500) | URL видео |
| is_preview | BOOLEAN | Доступен без покупки |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |
| updated_at | TIMESTAMP WITH TIME ZONE | Дата обновления |

**Индексы:**
- `idx_lessons_course_id` на `course_id`
- `idx_lessons_order_index` на `(course_id, order_index)`

**Constraints:**
- UNIQUE `(course_id, order_index)`

**Триггеры:**
- `update_lessons_updated_at` - автообновление `updated_at`
- `update_lessons_count` - обновление `lessons_count` в courses

#### lesson_contents
Контент уроков (текст, видео, код и т.д.).

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| lesson_id | UUID | FOREIGN KEY → lessons(id) |
| content_type | VARCHAR(20) | TEXT, VIDEO, CODE, IMAGE, QUIZ |
| content | TEXT | Содержимое (может быть JSON) |
| order_index | INT | Порядок элемента |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

**Индексы:**
- `idx_lesson_contents_lesson_id` на `lesson_id`

**Constraints:**
- UNIQUE `(lesson_id, order_index)`

---

### 3. Тесты и вопросы

#### tests
Тесты и экзамены.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| course_id | UUID | FOREIGN KEY → courses(id), NULLABLE |
| lesson_id | UUID | FOREIGN KEY → lessons(id), NULLABLE |
| title | VARCHAR(255) | Название теста |
| description | TEXT | Описание |
| passing_score | INT | Минимальный балл (0-100) |
| time_limit | INT | Лимит времени (минуты), NULL = без лимита |
| questions_count | INT | Количество вопросов |
| is_active | BOOLEAN | Активен ли тест |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |
| updated_at | TIMESTAMP WITH TIME ZONE | Дата обновления |

**Индексы:**
- `idx_tests_course_id` на `course_id`
- `idx_tests_lesson_id` на `lesson_id`
- `idx_tests_is_active` на `is_active`

**Constraints:**
- CHECK: `(course_id IS NOT NULL) OR (lesson_id IS NOT NULL)`
- CHECK: `passing_score >= 0 AND passing_score <= 100`

**Триггеры:**
- `update_tests_updated_at` - автообновление `updated_at`

#### questions
Вопросы тестов.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| test_id | UUID | FOREIGN KEY → tests(id) |
| question_text | TEXT | Текст вопроса |
| question_type | VARCHAR(30) | SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, TEXT |
| points | INT | Баллы за правильный ответ |
| order_index | INT | Порядок вопроса |
| explanation | TEXT | Пояснение к ответу |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

**Индексы:**
- `idx_questions_test_id` на `test_id`

**Constraints:**
- UNIQUE `(test_id, order_index)`

**Триггеры:**
- `update_questions_count` - обновление `questions_count` в tests

#### answers
Варианты ответов на вопросы.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| question_id | UUID | FOREIGN KEY → questions(id) |
| answer_text | TEXT | Текст ответа |
| is_correct | BOOLEAN | Правильный ли ответ |
| order_index | INT | Порядок ответа |

**Индексы:**
- `idx_answers_question_id` на `question_id`

**Constraints:**
- UNIQUE `(question_id, order_index)`

#### test_sessions
Сессии прохождения тестов пользователями.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| test_id | UUID | FOREIGN KEY → tests(id) |
| started_at | TIMESTAMP WITH TIME ZONE | Время начала |
| completed_at | TIMESTAMP WITH TIME ZONE | Время завершения |
| score | INT | Набранный балл |
| status | VARCHAR(20) | IN_PROGRESS, COMPLETED, EXPIRED |

**Индексы:**
- `idx_test_sessions_user_id` на `user_id`
- `idx_test_sessions_test_id` на `test_id`
- `idx_test_sessions_status` на `status`

#### user_test_answers
Ответы пользователей на вопросы тестов.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| session_id | UUID | FOREIGN KEY → test_sessions(id) |
| question_id | UUID | FOREIGN KEY → questions(id) |
| selected_answer_ids | UUID[] | Массив выбранных ответов |
| text_answer | TEXT | Текстовый ответ |
| is_correct | BOOLEAN | Правильный ли ответ |
| points_earned | INT | Заработанные баллы |
| answered_at | TIMESTAMP WITH TIME ZONE | Время ответа |

**Индексы:**
- `idx_user_test_answers_session_id` на `session_id`

**Constraints:**
- UNIQUE `(session_id, question_id)`

---

### 4. Спринты (промо-акции)

#### sprints
Промо-акции и специальные предложения.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| title | VARCHAR(255) | Название спринта |
| description | TEXT | Описание |
| cover_image | VARCHAR(500) | URL обложки |
| discount | INT | Процент скидки (0-100) |
| start_date | TIMESTAMP WITH TIME ZONE | Дата начала |
| end_date | TIMESTAMP WITH TIME ZONE | Дата окончания |
| is_active | BOOLEAN | Активен ли спринт |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

**Индексы:**
- `idx_sprints_is_active` на `is_active`
- `idx_sprints_dates` на `(start_date, end_date)`

**Constraints:**
- CHECK: `end_date > start_date`
- CHECK: `discount >= 0 AND discount <= 100`

#### sprint_courses
Связь между спринтами и курсами (many-to-many).

| Колонка | Тип | Описание |
|---------|-----|----------|
| sprint_id | UUID | FOREIGN KEY → sprints(id) |
| course_id | UUID | FOREIGN KEY → courses(id) |

**Primary Key:** `(sprint_id, course_id)`

**Индексы:**
- `idx_sprint_courses_sprint_id` на `sprint_id`
- `idx_sprint_courses_course_id` на `course_id`

---

### 5. Вопросы собеседования

#### interview_questions
Вопросы пользователей для подготовки к собеседованиям.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| category | VARCHAR(100) | Категория вопроса |
| question_text | TEXT | Текст вопроса |
| answer | TEXT | Ответ (заполняется позже) |
| difficulty | VARCHAR(20) | BEGINNER, INTERMEDIATE, ADVANCED |
| tags | VARCHAR(50)[] | Массив тегов |
| status | VARCHAR(20) | PENDING, ANSWERED, ARCHIVED |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |
| answered_at | TIMESTAMP WITH TIME ZONE | Дата ответа |

**Индексы:**
- `idx_interview_questions_user_id` на `user_id`
- `idx_interview_questions_status` на `status`
- `idx_interview_questions_category` на `category`

---

### 6. Роадмапы

#### roadmaps
Карьерные роадмапы для различных специальностей.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| title | VARCHAR(255) | Название роадмапа |
| description | TEXT | Описание |
| category | VARCHAR(100) | Категория (Android, iOS, Backend) |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

**Индексы:**
- `idx_roadmaps_category` на `category`

#### roadmap_stages
Этапы роадмапа.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| roadmap_id | UUID | FOREIGN KEY → roadmaps(id) |
| title | VARCHAR(255) | Название этапа |
| description | TEXT | Описание |
| order_index | INT | Порядок этапа |
| estimated_duration | INT | Оценка продолжительности (дни) |
| created_at | TIMESTAMP WITH TIME ZONE | Дата создания |

**Индексы:**
- `idx_roadmap_stages_roadmap_id` на `roadmap_id`

**Constraints:**
- UNIQUE `(roadmap_id, order_index)`

#### roadmap_stage_courses
Связь между этапами роадмапа и курсами (many-to-many).

| Колонка | Тип | Описание |
|---------|-----|----------|
| stage_id | UUID | FOREIGN KEY → roadmap_stages(id) |
| course_id | UUID | FOREIGN KEY → courses(id) |

**Primary Key:** `(stage_id, course_id)`

**Индексы:**
- `idx_roadmap_stage_courses_stage_id` на `stage_id`
- `idx_roadmap_stage_courses_course_id` на `course_id`

---

### 7. Прогресс пользователя

#### user_enrollments
Записи пользователей на курсы.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| course_id | UUID | FOREIGN KEY → courses(id) |
| enrolled_at | TIMESTAMP WITH TIME ZONE | Дата записи |

**Индексы:**
- `idx_user_enrollments_user_id` на `user_id`
- `idx_user_enrollments_course_id` на `course_id`

**Constraints:**
- UNIQUE `(user_id, course_id)`

#### user_progress
Прогресс прохождения курсов.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| course_id | UUID | FOREIGN KEY → courses(id) |
| lesson_id | UUID | FOREIGN KEY → lessons(id), NULLABLE |
| completion_percentage | INT | Процент завершения (0-100) |
| last_accessed_at | TIMESTAMP WITH TIME ZONE | Последний доступ |

**Индексы:**
- `idx_user_progress_user_id` на `user_id`
- `idx_user_progress_course_id` на `course_id`

**Constraints:**
- UNIQUE `(user_id, course_id)`
- CHECK: `completion_percentage >= 0 AND completion_percentage <= 100`

#### completed_lessons
Завершенные уроки.

| Колонка | Тип | Описание |
|---------|-----|----------|
| id | UUID | PRIMARY KEY |
| user_id | UUID | FOREIGN KEY → users(id) |
| lesson_id | UUID | FOREIGN KEY → lessons(id) |
| completed_at | TIMESTAMP WITH TIME ZONE | Дата завершения |

**Индексы:**
- `idx_completed_lessons_user_id` на `user_id`
- `idx_completed_lessons_lesson_id` на `lesson_id`

**Constraints:**
- UNIQUE `(user_id, lesson_id)`

---

## Триггеры и функции

### update_updated_at_column()
Автоматически обновляет поле `updated_at` при UPDATE.

**Применяется к:**
- users
- courses
- lessons
- tests

### update_course_lessons_count()
Автоматически обновляет счетчик уроков в курсе.

**Применяется к:** lessons (INSERT, DELETE)

### update_test_questions_count()
Автоматически обновляет счетчик вопросов в тесте.

**Применяется к:** questions (INSERT, DELETE)

---

## Типы данных и ENUM

### AuthProvider
- `LOCAL` - локальная аутентификация
- `GOOGLE` - OAuth через Google

### DifficultyLevel
- `BEGINNER` - начальный уровень
- `INTERMEDIATE` - средний уровень
- `ADVANCED` - продвинутый уровень

### ContentType
- `TEXT` - текстовый контент
- `VIDEO` - видео
- `CODE` - код
- `IMAGE` - изображение
- `QUIZ` - встроенный опрос

### QuestionType
- `SINGLE_CHOICE` - один правильный ответ
- `MULTIPLE_CHOICE` - несколько правильных ответов
- `TRUE_FALSE` - правда/ложь
- `TEXT` - текстовый ответ

### QuestionStatus (Interview)
- `PENDING` - ожидает ответа
- `ANSWERED` - отвечен
- `ARCHIVED` - архивирован

### SessionStatus
- `IN_PROGRESS` - в процессе
- `COMPLETED` - завершен
- `EXPIRED` - истек срок

---

## Примеры запросов

### Получить курсы пользователя с прогрессом
```sql
SELECT 
    c.*,
    ue.enrolled_at,
    up.completion_percentage,
    up.last_accessed_at
FROM courses c
JOIN user_enrollments ue ON c.id = ue.course_id
LEFT JOIN user_progress up ON c.id = up.course_id AND ue.user_id = up.user_id
WHERE ue.user_id = 'user-uuid'
ORDER BY ue.enrolled_at DESC;
```

### Получить уроки курса с отметками о завершении
```sql
SELECT 
    l.*,
    CASE WHEN cl.id IS NOT NULL THEN TRUE ELSE FALSE END as is_completed
FROM lessons l
LEFT JOIN completed_lessons cl ON l.id = cl.lesson_id AND cl.user_id = 'user-uuid'
WHERE l.course_id = 'course-uuid'
ORDER BY l.order_index;
```

### Получить активные спринты с курсами
```sql
SELECT 
    s.*,
    json_agg(json_build_object(
        'id', c.id,
        'title', c.title,
        'price', c.price,
        'discounted_price', c.price * (1 - s.discount / 100.0)
    )) as courses
FROM sprints s
JOIN sprint_courses sc ON s.id = sc.sprint_id
JOIN courses c ON sc.course_id = c.id
WHERE s.is_active = TRUE 
    AND s.start_date <= NOW() 
    AND s.end_date >= NOW()
GROUP BY s.id;
```

### Статистика пользователя
```sql
SELECT 
    COUNT(DISTINCT ue.course_id) as enrolled_courses,
    COUNT(DISTINCT CASE WHEN up.completion_percentage = 100 THEN up.course_id END) as completed_courses,
    COUNT(DISTINCT cl.lesson_id) as completed_lessons,
    AVG(CASE WHEN ts.status = 'COMPLETED' THEN ts.score END) as avg_test_score
FROM users u
LEFT JOIN user_enrollments ue ON u.id = ue.user_id
LEFT JOIN user_progress up ON u.id = up.user_id
LEFT JOIN completed_lessons cl ON u.id = cl.user_id
LEFT JOIN test_sessions ts ON u.id = ts.user_id
WHERE u.id = 'user-uuid'
GROUP BY u.id;
```

---

## Оптимизация

### Индексы для производительности
Все важные foreign keys имеют индексы для ускорения JOIN операций.

### Partitioning (будущее)
Для больших таблиц (user_test_answers, completed_lessons) рекомендуется партиционирование по времени.

### Кеширование
Часто запрашиваемые данные (список курсов, контент) кешируются в Redis.

---

## Безопасность

1. **Row Level Security (RLS)** - можно включить для дополнительной безопасности
2. **Хеширование паролей** - BCrypt с cost factor 12
3. **Токены** - используются UUID с ограниченным сроком действия
4. **Каскадное удаление** - настроено для всех зависимых записей

---

## Backup и восстановление

```bash
# Создать backup
pg_dump -U postgres learning_platform > backup.sql

# Восстановить
psql -U postgres learning_platform < backup.sql

# С Docker
docker-compose exec postgres pg_dump -U postgres learning_platform > backup.sql
cat backup.sql | docker-compose exec -T postgres psql -U postgres learning_platform
```

