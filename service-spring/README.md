# service-spring — сервис платформы обучения на Spring Boot

Модуль курса «Spring Boot на Kotlin». Тот же сервис, что в `service/` (Ktor), на Spring Boot 4.1,
Kotlin 2.4.20 и JDK 25. Каркас с `TODO`, все тесты курса сразу, тесты на старте красные.

| Что | Значение |
|---|---|
| Сервис Spring | `http://localhost:8081` (Ktor-двойник — 8080) |
| База Spring | PostgreSQL 18, порт **5433**, база `learning_spring` (Ktor — 5432) |
| Сервис в `docker-compose.yml` | `postgres` |
| Главный класс | `com.learning.spring.Application` (`@SpringBootApplication`, `@ConfigurationPropertiesScan`) |
| Миграции | Flyway `V1`–`V3`, как у Ktor-версии; учебные данные `db/seed` — только в профиле `dev` |

## Запуск

```bash
cd service-spring
docker compose up -d        # база Spring; дождитесь (healthy): docker compose ps
cd ..
./gradlew :service-spring:bootRun                  # или в IDE: Run Anything → gradle :service-spring:bootRun
./gradlew :service-spring:bootRun -Pprofile=dev    # профиль dev: открыт /actuator/beans, база с учебными данными
```

Профиль `dev` в IDE: переменная окружения `SPRING_PROFILES_ACTIVE=dev` в конфигурации запуска
(Edit Configurations → Environment variables) — то же, что `-Pprofile=dev`. В профиле `dev`
наружу открыты `health` и `beans`: `curl http://localhost:8081/actuator/beans`.

Секрет JWT берётся из переменной окружения `JWT_SECRET` (с урока 3). Значение — любая строка не
короче 32 знаков, например `local-dev-secret-at-least-32-chars!!`; задайте его в конфигурации запуска
рядом с `SPRING_PROFILES_ACTIVE` и не коммитьте. В профиле `test` тестовое значение лежит в
`application-test.yaml` (его создаёт урок 3).

Если порт 5433 или 8081 занят, остановите чужой контейнер или сервис: порты нужны курсу как есть.

Пока маршрут не написан, `curl -i http://localhost:8081/health` отвечает 500: запрос доходит до
заглушки `health/HealthStub.kt` и падает на `TODO` — это стартовое состояние (урок 1).

## Тесты и отчёт

```bash
./gradlew :service-spring:testReport -Pchapter=1   # → service-spring/reports/tests-01.txt
```

Тесты помечены тегами JUnit: `core` — обязательное ядро, `extension` — расширение, `star` — звёздочка,
у каждого теста ровно один `chapterN`. Отчёт показывает ядро глав 1–N; красные тесты следующих глав — норма.
Первая строка отчёта «Выполнено N из M»: если N меньше M, часть тестов пропущена. Отчёт детерминирован
(без даты, коммита и версии JDK): CI перегенерирует его и сверяет с закоммиченным.
Машина только с JDK 24: добавьте `-PjavaToolchain=24`.

| Глава | Ядро (классы тестов) | Расширение |
|---|---|---|
| 1 | `BeansContextTest`, `JwtPropertiesBindingTest`, `HealthProbesTest` | — |
| 2 | `CatalogRoutesTest`, `CatalogPagingTest`, `ValidationTest`, `SchemaValidationTest`, `ProgressTest` | остальные проверки в `ValidationTest` |
| 3 | `AccessRulesTest`, `AuthFlowTest`, `TokenCheckTest`, `RolesAndDraftsTest` | refresh и logout, уроки черновика |
| 4 | — (тесты пишет студент в `src/test/kotlin/com/learning/spring/student/`) | — |
| 5 | `SlugGeneratorTest`, `ProgressReportServiceTest` | — |
| 6 | полный прогон ядра, `tests-06.txt` | — |

Тесты ходят в сервис по HTTP и в базу по JDBC (PostgreSQL в Testcontainers, нужен Docker), поэтому
компилируются на чистом шаблоне. Маршруты — с префиксом `/api/v1`, как у Ktor-двойника; пробы — в корне.

## Что в каркасе

- `Application.kt` — точка входа; сканирование `@ConfigurationProperties` включено.
- `models/ApiModels.kt` — DTO и `ErrorResponse`, копия контракта Ktor-версии. Готово.
- `catalog/` — `CourseService`, `LessonService`, `CourseRepository` и его заглушка `InMemoryCourseRepository`
  без аннотаций; `ManualWiring` — ручная сборка (урок 2 её удаляет).
- `health/HealthStub.kt` — заглушка `/health` (урок 4 заменяет её Actuator).
- `legacy/` (Java) — материал главы 5: `SlugGenerator`, `ProgressReportService`, записи `ProgressEntry` и `CourseProgress`.
- `application.yaml` — порт, база, Flyway и закомментированные места под настройки уроков 3, 4, 5, 8.

## Как подключить модуль к существующему `backend-<логин>`

Модуль самодостаточен: версии плагинов Spring заданы в `service-spring/build.gradle.kts`, корневой
`build.gradle.kts` менять не нужно.

```bash
git remote add template https://github.com/AI-Sprints-School/backend-template.git   # если ещё нет
git fetch template
git checkout template/main -- service-spring .github/workflows/service-spring.yml
```

В `settings.gradle.kts` своего репозитория добавьте **отдельной строкой** (файл из шаблона не копируйте — он затрёт
ваши `include`):

```kotlin
include("service-spring")
```

Закоммитьте в `main`: `git add service-spring .github settings.gradle.kts && git commit -m "Подключить service-spring"`.
Ветки сдачи — `spring-NN`.
