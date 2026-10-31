# achievements-spring — сервис достижений

Второй сервис курса «Spring Boot на Kotlin», глава 6 (уроки 24–25). Читает событие «урок пройден»
из топика `lesson-completed`, который пишет `service-spring/`, и выдаёт достижение «курс пройден»,
когда студент прошёл последний урок курса. Сервисы связаны только событием: обратных вызовов
между ними нет, таблицы друг друга они не читают.

| Что | Значение |
|---|---|
| Сервис | `http://localhost:8082` (`service-spring` — 8081) |
| База | та же PostgreSQL на 5433, база `learning_spring`, **своя схема `achievements`** со своей историей Flyway (`achievements.flyway_schema_history`) |
| Брокер | `localhost:9092` — `docker compose --profile kafka up -d` в `service-spring/` |
| Топики | `lesson-completed` (3 партиции, создаёт `service-spring`), `lesson-completed-dlt` — недоставленные |
| Главный класс | `com.learning.achievements.Application` |
| Миграции | `V1__achievements.sql` — таблицы `achievements` (одно достижение на пару `user_id` + `course_id`) и `processed_events` (первичный ключ `event_id`) |

## Запуск

```bash
cd service-spring && docker compose --profile kafka up -d && cd ..
./gradlew :service-spring:bootRun          # первый сервис — пишет событие
./gradlew :achievements-spring:bootRun     # второй — в другом терминале
```

## Контракт события

`events/LessonCompletedEvent.kt` — копия класса из `service-spring/…/events/`: `eventId`, `userId`,
`courseId`, `lessonId`, `completedLessons`, `totalLessons`, `occurredAt`. Ключ записи — `userId`,
значение — JSON без заголовков типа (пакеты у копий разные, тип задаёт получатель).

Корректное событие: `totalLessons > 0` и `completedLessons` от 1 до `totalLessons`. Некорректное
обработчик отклоняет исключением, и после повторов оно уходит в `lesson-completed-dlt`.

## Тесты

Тесты поднимают PostgreSQL и Kafka в Testcontainers (нужен Docker) и подключают их через
`@ServiceConnection`. Отдельного отчёта у модуля нет: `./gradlew :service-spring:testReport -Pchapter=6`
прогоняет оба сервиса и пишет один `service-spring/reports/tests-06.txt`.

| Урок | Ядро (классы тестов) |
|---|---|
| 24 | `AchievementConsumerTest` — достижение за последний урок, повтор события, два финальных события |
| 25 | `DeadLetterTest` — битый JSON и некорректное событие уходят в DLT, следующая запись обрабатывается |
| 25 | свой тест доставки — `src/test/kotlin/com/learning/achievements/student/` |

## Что в каркасе

- `Application.kt`, `application.yaml` (порт, база, схема Flyway, адрес брокера) — готово.
- `events/LessonCompletedEvent.kt` — копия контракта. Готово.
- Консьюмер, обработчик ошибок и сущности пишет студент (уроки 24–25).
