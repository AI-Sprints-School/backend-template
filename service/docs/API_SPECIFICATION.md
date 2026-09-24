# Спецификация Backend API
## Мобильное приложение для обучения

**Технологический стек:**
- Kotlin
- Ktor Framework
- PostgreSQL
- Docker Compose
- JWT для авторизации

---

## 1. Архитектура системы

### 1.1 Компоненты
- **API Server** (Ktor) - порт 8080
- **PostgreSQL Database** - порт 5432
- **Redis** (для кеширования сессий) - порт 6379
- **SMTP Server** (для отправки email)

### 1.2 Аутентификация
- Bearer Token (JWT)
- ~~OAuth 2.0 (Google)~~ — **не реализовано** (см. §3.1.4)
- Refresh Token механизм

---

## 2. Модели данных (Entity Models)

### 2.1 User (Пользователь)
```kotlin
data class User(
    val id: UUID,
    val email: String,
    val password: String?, // null зарезервирован под OAuth — OAuth не реализован
    val firstName: String,
    val lastName: String,
    val profilePhoto: String?,
    val authProvider: AuthProvider, // сейчас только LOCAL; GOOGLE — не реализовано
    val emailVerified: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class AuthProvider {
    LOCAL, GOOGLE
}
```

### 2.2 Course (Курс)
```kotlin
data class Course(
    val id: UUID,
    val title: String,
    val description: String,
    val coverImage: String,
    val category: String,
    val difficulty: DifficultyLevel, // BEGINNER, INTERMEDIATE, ADVANCED
    val duration: Int, // в часах
    val lessonsCount: Int,
    val rating: Float,
    val studentsCount: Int,
    val price: BigDecimal,
    val isPremium: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class DifficultyLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}
```

### 2.3 Lesson (Урок)
```kotlin
data class Lesson(
    val id: UUID,
    val courseId: UUID,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val duration: Int, // в минутах
    val videoUrl: String?,
    val isPreview: Boolean, // доступен без покупки курса
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### 2.4 LessonContent (Контент урока)
```kotlin
data class LessonContent(
    val id: UUID,
    val lessonId: UUID,
    val contentType: ContentType, // TEXT, VIDEO, CODE, IMAGE
    val content: String, // JSON или текст
    val orderIndex: Int,
    val createdAt: Instant
)

enum class ContentType {
    TEXT, VIDEO, CODE, IMAGE, QUIZ
}
```

### 2.5 Test (Тест)
```kotlin
data class Test(
    val id: UUID,
    val courseId: UUID?,
    val lessonId: UUID?,
    val title: String,
    val description: String,
    val passingScore: Int, // минимальный балл для прохождения (%)
    val timeLimit: Int?, // в минутах, null = без ограничений
    val questionsCount: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### 2.6 Question (Вопрос)
```kotlin
data class Question(
    val id: UUID,
    val testId: UUID,
    val questionText: String,
    val questionType: QuestionType, // SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, TEXT
    val points: Int,
    val orderIndex: Int,
    val explanation: String?,
    val createdAt: Instant
)

enum class QuestionType {
    SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, TEXT
}
```

### 2.7 Answer (Вариант ответа)
```kotlin
data class Answer(
    val id: UUID,
    val questionId: UUID,
    val answerText: String,
    val isCorrect: Boolean,
    val orderIndex: Int
)
```

### 2.8 UserTestAnswer (Ответ пользователя)
```kotlin
data class UserTestAnswer(
    val id: UUID,
    val userId: UUID,
    val testId: UUID,
    val questionId: UUID,
    val selectedAnswerIds: List<UUID>, // для MULTIPLE_CHOICE
    val textAnswer: String?, // для TEXT
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val answeredAt: Instant
)
```

### 2.9 Sprint (Актуальный спринт/Промо)
```kotlin
data class Sprint(
    val id: UUID,
    val title: String,
    val description: String,
    val coverImage: String,
    val discount: Int?, // процент скидки
    val startDate: Instant,
    val endDate: Instant,
    val courseIds: List<UUID>, // курсы, участвующие в спринте
    val isActive: Boolean,
    val createdAt: Instant
)
```

### 2.10 InterviewQuestion (Вопрос собеседования)
```kotlin
data class InterviewQuestion(
    val id: UUID,
    val userId: UUID,
    val category: String,
    val questionText: String,
    val answer: String?,
    val difficulty: DifficultyLevel,
    val tags: List<String>,
    val status: QuestionStatus, // PENDING, ANSWERED, ARCHIVED
    val createdAt: Instant,
    val answeredAt: Instant?
)

enum class QuestionStatus {
    PENDING, ANSWERED, ARCHIVED
}
```

### 2.11 Roadmap (Роадмап)
```kotlin
data class Roadmap(
    val id: UUID,
    val title: String,
    val description: String,
    val category: String, // Android, iOS, Backend, etc.
    val stages: List<RoadmapStage>,
    val createdAt: Instant
)

data class RoadmapStage(
    val id: UUID,
    val roadmapId: UUID,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val courseIds: List<UUID>,
    val estimatedDuration: Int // в днях
)
```

### 2.12 UserProgress (Прогресс пользователя)
```kotlin
data class UserProgress(
    val id: UUID,
    val userId: UUID,
    val courseId: UUID,
    val lessonId: UUID?,
    val completionPercentage: Int,
    val lastAccessedAt: Instant
)
```

---

## 3. API Endpoints

### 3.1 Аутентификация и Авторизация

#### 3.1.1 Регистрация
```
POST /api/v1/auth/register
Content-Type: application/json

Request:
{
    "email": "user@example.com",
    "password": "securePassword123",
    "firstName": "Иван",
    "lastName": "Иванов"
}

Response: 201 Created
{
    "message": "Регистрация успешна. Проверьте email для подтверждения.",
    "userId": "uuid"
}
```

#### 3.1.2 Подтверждение Email
```
GET /api/v1/auth/verify-email?token={verificationToken}

Response: 200 OK
{
    "message": "Email успешно подтвержден"
}
```

#### 3.1.3 Вход (логин/пароль)
```
POST /api/v1/auth/login
Content-Type: application/json

Request:
{
    "email": "user@example.com",
    "password": "securePassword123"
}

Response: 200 OK
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "refresh_token_here",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
        "id": "uuid",
        "email": "user@example.com",
        "firstName": "Иван",
        "lastName": "Иванов",
        "profilePhoto": "url"
    }
}

Errors:
401 Unauthorized - неверные credentials
403 Forbidden - email не подтвержден
```

#### 3.1.4 OAuth Google — не реализовано

Вход через Google в сервисе **не реализован**: маршрутов `/auth/oauth/google`
нет ни в шаблоне, ни в эталоне, переменные `GOOGLE_*` сервис не читает.
В таблице `users` остался только столбец `google_id` — задел под будущую
доработку. Раздел сохранён, чтобы номера следующих разделов не сдвигались.

#### 3.1.5 Обновление токена
```
POST /api/v1/auth/refresh
Content-Type: application/json

Request:
{
    "refreshToken": "refresh_token_here"
}

Response: 200 OK
{
    "accessToken": "new_jwt_token",
    "refreshToken": "new_refresh_token",
    "tokenType": "Bearer",
    "expiresIn": 3600
}
```

#### 3.1.6 Сброс пароля (запрос)
```
POST /api/v1/auth/password/reset-request
Content-Type: application/json

Request:
{
    "email": "user@example.com"
}

Response: 200 OK
{
    "message": "Инструкции отправлены на email"
}
```

#### 3.1.7 Сброс пароля (подтверждение)
```
POST /api/v1/auth/password/reset
Content-Type: application/json

Request:
{
    "token": "reset_token",
    "newPassword": "newSecurePassword123"
}

Response: 200 OK
{
    "message": "Пароль успешно изменен"
}
```

#### 3.1.8 Выход
```
POST /api/v1/auth/logout
Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Выход выполнен успешно"
}
```

---

### 3.2 Курсы

#### 3.2.1 Получить список курсов
```
GET /api/v1/courses?page=1&limit=20&category=android&difficulty=beginner&search=kotlin
Authorization: Bearer {token} (optional)

Query Parameters:
- page: номер страницы (default: 1)
- limit: количество элементов (default: 20, max: 100)
- category: фильтр по категории
- difficulty: beginner|intermediate|advanced
- search: поиск по названию/описанию
- isPremium: true|false
- sortBy: title|rating|price|studentsCount (default: createdAt)
- sortOrder: asc|desc (default: desc)

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "title": "Kotlin для Android",
            "description": "Полный курс...",
            "coverImage": "https://...",
            "category": "Android",
            "difficulty": "BEGINNER",
            "duration": 40,
            "lessonsCount": 25,
            "rating": 4.8,
            "studentsCount": 1543,
            "price": 2990.00,
            "isPremium": true,
            "userProgress": {
                "isEnrolled": true,
                "completionPercentage": 45
            }
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 20,
        "total": 156,
        "totalPages": 8
    }
}
```

#### 3.2.2 Получить курс по ID
```
GET /api/v1/courses/{courseId}
Authorization: Bearer {token} (optional)

Response: 200 OK
{
    "id": "uuid",
    "title": "Kotlin для Android",
    "description": "Полный курс...",
    "coverImage": "https://...",
    "category": "Android",
    "difficulty": "BEGINNER",
    "duration": 40,
    "lessonsCount": 25,
    "rating": 4.8,
    "studentsCount": 1543,
    "price": 2990.00,
    "isPremium": true,
    "syllabus": "Программа курса...",
    "requirements": ["Базовые знания программирования"],
    "learningOutcomes": ["Научитесь создавать Android приложения"],
    "instructor": {
        "id": "uuid",
        "name": "Иван Петров",
        "bio": "10 лет опыта",
        "photo": "url"
    },
    "userProgress": {
        "isEnrolled": true,
        "completionPercentage": 45,
        "lastAccessedLesson": "uuid"
    }
}
```

Черновик (`isPublished = false`) видят только автор курса и роль `admin`: им —
`200 OK`, всем остальным, в том числе без токена, — `404 NOT_FOUND`, как у
несуществующего курса. `403` здесь не отвечают: он подтвердил бы, что курс есть.
Каталог `GET /courses` черновиков не содержит ни для кого.

#### 3.2.3 Получить контент курса (список уроков)
```
GET /api/v1/courses/{courseId}/lessons
Authorization: Bearer {token}

Response: 200 OK
{
    "courseId": "uuid",
    "lessons": [
        {
            "id": "uuid",
            "title": "Введение в Kotlin",
            "description": "Основы языка",
            "orderIndex": 1,
            "duration": 15,
            "videoUrl": "https://...",
            "isPreview": true,
            "isCompleted": true,
            "isLocked": false
        }
    ]
}
```

---

### 3.3 Уроки

#### 3.3.1 Получить урок
```
GET /api/v1/lessons/{lessonId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": "uuid",
    "courseId": "uuid",
    "title": "Введение в Kotlin",
    "description": "Основы языка",
    "orderIndex": 1,
    "duration": 15,
    "videoUrl": "https://...",
    "isPreview": false,
    "content": [
        {
            "id": "uuid",
            "contentType": "TEXT",
            "content": "Текст урока...",
            "orderIndex": 1
        },
        {
            "id": "uuid",
            "contentType": "VIDEO",
            "content": "https://video.url",
            "orderIndex": 2
        },
        {
            "id": "uuid",
            "contentType": "CODE",
            "content": "fun main() { println(\"Hello\") }",
            "orderIndex": 3
        }
    ],
    "nextLesson": {
        "id": "uuid",
        "title": "Переменные и типы данных"
    },
    "previousLesson": null
}

Errors:
403 Forbidden - урок недоступен (курс не куплен)
404 Not Found - урок не найден
```

#### 3.3.2 Отметить урок как завершенный
```
POST /api/v1/lessons/{lessonId}/complete
Authorization: Bearer {token}

Response: 200 OK
{
    "message": "Урок отмечен как завершенный",
    "progress": {
        "courseCompletionPercentage": 48
    }
}
```

---

### 3.4 Спринты (Промо акции)

#### 3.4.1 Список актуальных спринтов
```
GET /api/v1/sprints?active=true
Authorization: Bearer {token} (optional)

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "title": "Весенний марафон",
            "description": "Скидки до 50%",
            "coverImage": "https://...",
            "discount": 50,
            "startDate": "2025-03-01T00:00:00Z",
            "endDate": "2025-03-31T23:59:59Z",
            "daysLeft": 12,
            "courses": [
                {
                    "id": "uuid",
                    "title": "Kotlin для Android",
                    "originalPrice": 2990.00,
                    "discountedPrice": 1495.00
                }
            ],
            "isActive": true
        }
    ]
}
```

---

### 3.5 Тесты

#### 3.5.1 Список тестов
```
GET /api/v1/tests?courseId={courseId}&page=1&limit=20
Authorization: Bearer {token}

Query Parameters:
- courseId: фильтр по курсу (optional)
- lessonId: фильтр по уроку (optional)
- page, limit: пагинация

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "courseId": "uuid",
            "lessonId": null,
            "title": "Финальный тест по Kotlin",
            "description": "Проверьте свои знания",
            "passingScore": 70,
            "timeLimit": 60,
            "questionsCount": 20,
            "userAttempts": 2,
            "bestScore": 85,
            "isActive": true
        }
    ],
    "pagination": { ... }
}
```

#### 3.5.2 Получить контент теста
```
GET /api/v1/tests/{testId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": "uuid",
    "title": "Финальный тест по Kotlin",
    "description": "Проверьте свои знания",
    "passingScore": 70,
    "timeLimit": 60,
    "questionsCount": 20,
    "questions": [
        {
            "id": "uuid",
            "questionText": "Что такое Kotlin?",
            "questionType": "SINGLE_CHOICE",
            "points": 5,
            "orderIndex": 1,
            "answers": [
                {
                    "id": "uuid",
                    "answerText": "Язык программирования",
                    "orderIndex": 1
                },
                {
                    "id": "uuid",
                    "answerText": "База данных",
                    "orderIndex": 2
                }
            ]
        }
    ],
    "userProgress": {
        "attempts": 2,
        "bestScore": 85,
        "lastAttemptDate": "2025-09-15T10:30:00Z"
    }
}
```

#### 3.5.3 Начать тест
```
POST /api/v1/tests/{testId}/start
Authorization: Bearer {token}

Response: 200 OK
{
    "sessionId": "uuid",
    "testId": "uuid",
    "startedAt": "2025-10-01T12:00:00Z",
    "expiresAt": "2025-10-01T13:00:00Z",
    "questions": [ ... ]
}
```

#### 3.5.4 Отправить ответ на вопрос
```
POST /api/v1/tests/{testId}/answers
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "sessionId": "uuid",
    "questionId": "uuid",
    "selectedAnswerIds": ["uuid1", "uuid2"], // для MULTIPLE_CHOICE
    "textAnswer": "текстовый ответ" // для TEXT
}

Response: 200 OK
{
    "isCorrect": true,
    "pointsEarned": 5,
    "explanation": "Правильно! Kotlin - это язык программирования..."
}
```

#### 3.5.5 Завершить тест
```
POST /api/v1/tests/{testId}/submit
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "sessionId": "uuid"
}

Response: 200 OK
{
    "score": 85,
    "totalPoints": 100,
    "correctAnswers": 17,
    "totalQuestions": 20,
    "passed": true,
    "timeTaken": 45, // минут
    "detailedResults": [
        {
            "questionId": "uuid",
            "isCorrect": true,
            "pointsEarned": 5,
            "explanation": "..."
        }
    ]
}
```

---

### 3.6 Вопросы (пагинация)

#### 3.6.1 Получить список вопросов теста
```
GET /api/v1/tests/{testId}/questions?page=1&limit=10
Authorization: Bearer {token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "questionText": "Что такое Kotlin?",
            "questionType": "SINGLE_CHOICE",
            "points": 5,
            "orderIndex": 1,
            "answers": [ ... ]
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 10,
        "total": 20,
        "totalPages": 2
    }
}
```

---

### 3.7 Роадмап

#### 3.7.1 Получить роадмап
```
GET /api/v1/roadmap?category=android
Authorization: Bearer {token} (optional)

Response: 200 OK
{
    "id": "uuid",
    "title": "Android Developer Roadmap",
    "description": "Путь от новичка до эксперта",
    "category": "Android",
    "stages": [
        {
            "id": "uuid",
            "title": "Основы",
            "description": "Изучите основы программирования",
            "orderIndex": 1,
            "courses": [
                {
                    "id": "uuid",
                    "title": "Kotlin Basics",
                    "isCompleted": true
                }
            ],
            "estimatedDuration": 30,
            "completionPercentage": 100
        }
    ],
    "userProgress": {
        "overallCompletion": 35,
        "currentStageIndex": 2
    }
}
```

---

### 3.8 Вопросы собеседования

#### 3.8.1 Отправить вопрос собеседования
```
POST /api/v1/interview-questions
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "category": "Android",
    "questionText": "Как работает Activity Lifecycle?",
    "difficulty": "INTERMEDIATE",
    "tags": ["android", "activity", "lifecycle"]
}

Response: 201 Created
{
    "id": "uuid",
    "category": "Android",
    "questionText": "Как работает Activity Lifecycle?",
    "difficulty": "INTERMEDIATE",
    "tags": ["android", "activity", "lifecycle"],
    "status": "PENDING",
    "createdAt": "2025-10-01T12:00:00Z"
}
```

#### 3.8.2 Получить список вопросов собеседования
```
GET /api/v1/interview-questions?page=1&limit=20&category=android&status=pending
Authorization: Bearer {token}

Response: 200 OK
{
    "data": [
        {
            "id": "uuid",
            "category": "Android",
            "questionText": "Как работает Activity Lifecycle?",
            "answer": "Activity имеет следующие состояния...",
            "difficulty": "INTERMEDIATE",
            "tags": ["android", "activity", "lifecycle"],
            "status": "ANSWERED",
            "createdAt": "2025-09-20T10:00:00Z",
            "answeredAt": "2025-09-21T14:30:00Z"
        }
    ],
    "pagination": { ... }
}
```

#### 3.8.3 Получить вопрос по ID
```
GET /api/v1/interview-questions/{questionId}
Authorization: Bearer {token}

Response: 200 OK
{
    "id": "uuid",
    "category": "Android",
    "questionText": "Как работает Activity Lifecycle?",
    "answer": "Activity имеет следующие состояния: onCreate(), onStart(), onResume()...",
    "difficulty": "INTERMEDIATE",
    "tags": ["android", "activity", "lifecycle"],
    "status": "ANSWERED",
    "createdAt": "2025-09-20T10:00:00Z",
    "answeredAt": "2025-09-21T14:30:00Z"
}
```

---

### 3.9 Профиль пользователя

#### 3.9.1 Получить профиль
```
GET /api/v1/users/me
Authorization: Bearer {token}

Response: 200 OK
{
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "Иван",
    "lastName": "Иванов",
    "profilePhoto": "https://...",
    "authProvider": "LOCAL",
    "emailVerified": true,
    "enrolledCourses": 5,
    "completedCourses": 2,
    "totalLearningTime": 4500, // минут
    "createdAt": "2025-01-15T10:00:00Z"
}
```

#### 3.9.2 Обновить профиль
```
PATCH /api/v1/users/me
Authorization: Bearer {token}
Content-Type: application/json

Request:
{
    "firstName": "Петр",
    "lastName": "Сидоров",
    "profilePhoto": "https://..."
}

Response: 200 OK
{
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "Петр",
    "lastName": "Сидоров",
    "profilePhoto": "https://...",
    ...
}
```

---

## 4. Схема базы данных PostgreSQL

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    profile_photo VARCHAR(500),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Email verification tokens
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Courses table
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    cover_image VARCHAR(500),
    category VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    duration INT NOT NULL, -- hours
    lessons_count INT DEFAULT 0,
    rating DECIMAL(3, 2) DEFAULT 0,
    students_count INT DEFAULT 0,
    price DECIMAL(10, 2) NOT NULL,
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courses_category ON courses(category);
CREATE INDEX idx_courses_difficulty ON courses(difficulty);

-- Lessons table
CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INT NOT NULL,
    duration INT NOT NULL, -- minutes
    video_url VARCHAR(500),
    is_preview BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lessons_course_id ON lessons(course_id);

-- Lesson content table
CREATE TABLE lesson_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    order_index INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lesson_contents_lesson_id ON lesson_contents(lesson_id);

-- Tests table
CREATE TABLE tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    passing_score INT NOT NULL,
    time_limit INT,
    questions_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Questions table
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    test_id UUID NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    points INT NOT NULL,
    order_index INT NOT NULL,
    explanation TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_test_id ON questions(test_id);

-- Answers table
CREATE TABLE answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    order_index INT NOT NULL
);

CREATE INDEX idx_answers_question_id ON answers(question_id);

-- User test sessions
CREATE TABLE test_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    test_id UUID NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    score INT,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' -- IN_PROGRESS, COMPLETED, EXPIRED
);

-- User test answers
CREATE TABLE user_test_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    selected_answer_ids UUID[] NOT NULL,
    text_answer TEXT,
    is_correct BOOLEAN NOT NULL,
    points_earned INT NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sprints table
CREATE TABLE sprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    discount INT,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Sprint courses (many-to-many)
CREATE TABLE sprint_courses (
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    PRIMARY KEY (sprint_id, course_id)
);

-- Interview questions table
CREATE TABLE interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    question_text TEXT NOT NULL,
    answer TEXT,
    difficulty VARCHAR(20) NOT NULL,
    tags VARCHAR(50)[],
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_interview_questions_user_id ON interview_questions(user_id);
CREATE INDEX idx_interview_questions_status ON interview_questions(status);

-- Roadmaps table
CREATE TABLE roadmaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roadmap stages
CREATE TABLE roadmap_stages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    roadmap_id UUID NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    order_index INT NOT NULL,
    estimated_duration INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roadmap stage courses (many-to-many)
CREATE TABLE roadmap_stage_courses (
    stage_id UUID NOT NULL REFERENCES roadmap_stages(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    PRIMARY KEY (stage_id, course_id)
);

-- User enrollments
CREATE TABLE user_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, course_id)
);

CREATE INDEX idx_user_enrollments_user_id ON user_enrollments(user_id);

-- User progress
CREATE TABLE user_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id UUID REFERENCES lessons(id) ON DELETE CASCADE,
    completion_percentage INT DEFAULT 0,
    last_accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, course_id)
);

CREATE INDEX idx_user_progress_user_id ON user_progress(user_id);

-- Completed lessons
CREATE TABLE completed_lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, lesson_id)
);
```

---

## 5. Docker Compose конфигурация

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: backend_postgres
    environment:
      POSTGRES_DB: learning_platform
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    networks:
      - backend_network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: backend_redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - backend_network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: backend_app
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/learning_platform
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres_password
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: your_jwt_secret_key_here
      JWT_ISSUER: learning-platform
      JWT_AUDIENCE: learning-platform-api
      JWT_EXPIRATION: 3600000
      REFRESH_TOKEN_EXPIRATION: 2592000000
      SMTP_HOST: smtp.gmail.com
      SMTP_PORT: 587
      SMTP_USERNAME: your_email@gmail.com
      SMTP_PASSWORD: your_app_password
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - backend_network
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:

networks:
  backend_network:
    driver: bridge
```

---

## 6. Структура проекта Ktor

```
backend-starter-course/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/
│   │   │       └── learning/
│   │   │           ├── Application.kt
│   │   │           ├── config/
│   │   │           │   ├── DatabaseConfig.kt
│   │   │           │   ├── RedisConfig.kt
│   │   │           │   ├── SecurityConfig.kt
│   │   │           │   └── EmailConfig.kt
│   │   │           ├── di/
│   │   │           │   └── AppModule.kt
│   │   │           ├── domain/
│   │   │           │   ├── models/
│   │   │           │   └── repositories/
│   │   │           ├── data/
│   │   │           │   ├── entities/
│   │   │           │   └── repositories/
│   │   │           ├── routes/
│   │   │           │   ├── AuthRoutes.kt
│   │   │           │   ├── CourseRoutes.kt
│   │   │           │   ├── LessonRoutes.kt
│   │   │           │   ├── TestRoutes.kt
│   │   │           │   ├── SprintRoutes.kt
│   │   │           │   ├── InterviewRoutes.kt
│   │   │           │   ├── RoadmapRoutes.kt
│   │   │           │   └── UserRoutes.kt
│   │   │           ├── services/
│   │   │           │   ├── AuthService.kt
│   │   │           │   ├── CourseService.kt
│   │   │           │   ├── LessonService.kt
│   │   │           │   ├── TestService.kt
│   │   │           │   ├── EmailService.kt
│   │   │           │   └── JwtService.kt
│   │   │           ├── plugins/
│   │   │           │   ├── Routing.kt
│   │   │           │   ├── Security.kt
│   │   │           │   ├── Serialization.kt
│   │   │           │   ├── StatusPages.kt
│   │   │           │   └── CORS.kt
│   │   │           └── utils/
│   │   │               ├── Extensions.kt
│   │   │               └── Validators.kt
│   │   └── resources/
│   │       ├── application.conf
│   │       └── logback.xml
│   └── test/
│       └── kotlin/
│           └── com/
│               └── learning/
│                   └── ApplicationTest.kt
├── build.gradle.kts
├── Dockerfile
├── docker-compose.yml
├── init-db.sql
└── README.md
```

---

## 7. Безопасность

### 7.1 JWT токены
- **Access Token**: срок действия 1 час
- **Refresh Token**: срок действия 30 дней
- Алгоритм: HS256
- Хранение Refresh Token — в таблице `refresh_tokens`, не сам токен, а его SHA-256

### 7.2 Пароли
- Хеширование: BCrypt (cost factor 12)
- Длина: от 8 до 100 символов
- Требования: заглавная и строчная латинская буква, цифра — одно правило для регистрации и сброса пароля (`PasswordPolicy` в `Validators.kt`)

### 7.3 Rate Limiting
- Лимит считается отдельно для каждого IP-адреса клиента
- Зона `auth` — 5 запросов в минуту (`RATE_LIMIT_AUTH`): регистрация, вход, обновление токена, выход, подтверждение почты, сброс пароля
- Зона `api` — 100 запросов в минуту (`RATE_LIMIT_API`): остальные маршруты, включая `GET /auth/me`
- Превышение — `429 Too Many Requests` с заголовком `Retry-After` и телом ошибки `RATE_LIMIT_EXCEEDED` (§8)

### 7.4 CORS
- Разрешенные origins настраиваются через переменные окружения
- Разрешенные методы: GET, POST, PUT, PATCH, DELETE
- Разрешенные headers: Authorization, Content-Type

---

## 8. Коды ошибок

Тело любой ошибки — плоский объект `ErrorResponse`, одинаковый для всех
маршрутов и для ответов плагинов (`StatusPages`, `RequestValidation`):

```json
{
    "error": "VALIDATION_ERROR",
    "message": "Ошибка валидации данных",
    "details": "email: Некорректный формат email; password: Пароль должен содержать минимум 8 символов"
}
```

- `error` — код ошибки из списка ниже, строка;
- `message` — описание для человека;
- `details` — необязательная строка с подробностями (для `VALIDATION_ERROR` —
  все нарушения через `; `), в остальных ошибках отсутствует или `null`.

### Коды ошибок:
- `VALIDATION_ERROR` - 400: Запрос не прошёл проверку формата (валидаторы, `AppException.ValidationError`)
- `BAD_REQUEST` - 400: Некорректный запрос (неразбираемое тело, неверный UUID в пути, невалидный токен сброса)
- `UNAUTHORIZED` - 401: Нет токена или токен недействителен (ответ плагина аутентификации)
- `AUTHENTICATION_ERROR` - 401: Неверные учётные данные или refresh-токен
- `AUTHORIZATION_ERROR` - 403: Нет нужной роли
- `NOT_FOUND` - 404: Ресурс не найден
- `CONFLICT` - 409: Конфликт с данными, например почта уже зарегистрирована
- `RATE_LIMIT_EXCEEDED` - 429: Превышен лимит запросов, есть заголовок `Retry-After`
- `INTERNAL_ERROR` - 500: Внутренняя ошибка сервера
- `DATABASE_ERROR` - 500: Ошибка базы данных

---

## 9. Переменные окружения

```env
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/learning_platform
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres_password
DATABASE_MAX_POOL_SIZE=10

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your_secret_key_change_in_production
JWT_ISSUER=learning-platform
JWT_AUDIENCE=learning-platform-api
JWT_EXPIRATION=3600000
REFRESH_TOKEN_EXPIRATION=2592000000

# Email (SMTP)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=noreply@learning-platform.com

# Server
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# CORS
ALLOWED_ORIGINS=http://localhost:3000,https://your-frontend.com

# File Storage
FILE_STORAGE_PATH=/app/uploads
MAX_FILE_SIZE=10485760

# Logging
LOG_LEVEL=INFO
```

---

## 10. Зависимости (build.gradle.kts)

```kotlin
val ktor_version = "2.3.5"
val kotlin_version = "1.9.20"
val logback_version = "1.4.11"
val postgres_version = "42.6.0"
val exposed_version = "0.44.0"

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    
    // Ktor Plugins
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    
    // Database
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Redis
    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")
    
    // Security
    implementation("at.favre.lib:bcrypt:0.10.2")
    
    // Email
    implementation("org.simplejavamail:simple-java-mail:8.3.1")
    
    // Validation
    implementation("io.konform:konform-jvm:0.4.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

---

## 11. Пример Dockerfile

```dockerfile
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . /app
RUN gradle buildFatJar --no-daemon

FROM openjdk:17-jdk-slim
EXPOSE 8080
RUN mkdir /app
COPY --from=build /app/build/libs/*.jar /app/application.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

---

## 12. Makefile для управления

```makefile
.PHONY: build up down restart logs test clean

build:
	docker-compose build

up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f backend

db-logs:
	docker-compose logs -f postgres

test:
	./gradlew test

clean:
	docker-compose down -v
	./gradlew clean

migrate:
	docker-compose exec postgres psql -U postgres -d learning_platform -f /docker-entrypoint-initdb.d/init-db.sql

shell:
	docker-compose exec backend sh

db-shell:
	docker-compose exec postgres psql -U postgres -d learning_platform
```

---

## 13. Примечания по реализации

### 13.1 Пагинация
Все списковые endpoints поддерживают пагинацию:
- `page`: номер страницы (начинается с 1)
- `limit`: количество элементов на странице (default: 20, max: 100)

### 13.2 Фильтрация и сортировка
Многие endpoints поддерживают фильтрацию и сортировку через query параметры.

### 13.3 Загрузка файлов
Для загрузки изображений профиля и других файлов используйте multipart/form-data.

### 13.4 Локализация
API поддерживает header `Accept-Language` для локализации ответов (ru, en).

### 13.5 Версионирование API
API использует версионирование через URL: `/api/v1/...`
