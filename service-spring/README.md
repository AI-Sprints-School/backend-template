# service-spring — сервис платформы обучения на Spring Boot

Модуль курса «Spring Boot на Kotlin». Тот же сервис, что в `service/` (Ktor), на Spring Boot 4.1,
Kotlin 2.4.20 и JDK 25. Каркас с `TODO`, все тесты курса сразу, тесты на старте красные.

| Что | Значение |
|---|---|
| Сервис Spring | `http://localhost:8081` (Ktor-двойник — 8080) |
| База Spring | PostgreSQL 18, порт **5433**, база `learning_spring` (Ktor — 5432) |
| Сервисы в `docker-compose.yml` | `postgres`; `keycloak` (профиль `oauth`, порт 8180, урок 15); `kafka` (профиль `kafka`, порт 9092, глава 6) |
| Второй сервис (глава 6) | `achievements-spring/` в корне репозитория, порт 8082 — свой README |
| Главный класс | `com.learning.spring.Application` (`@SpringBootApplication`, `@ConfigurationPropertiesScan`) |
| Миграции | Flyway `V1`–`V3`, как у Ktor-версии, и `V4` — только Spring (урок 15); учебные данные `db/seed` — только в профиле `dev` |

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

### Keycloak — урок 15

```bash
cd service-spring
docker compose --profile oauth up -d     # база и Keycloak; первый старт — около минуты
```

Realm, клиент и пользователи импортируются из `keycloak/realm-learning.json` при старте контейнера.
Значения ниже — **тестовые**, только для учебного Keycloak на своей машине; в настоящем провайдере
секреты не коммитят.

| Что | Тестовое значение |
|---|---|
| Консоль администратора | `http://localhost:8180/admin`, вход `admin` / `admin-local-pass` |
| Realm | `learning` |
| Клиент | `service-spring`, конфиденциальный, секрет `service-spring-local-secret` |
| Адрес возврата клиента | `http://localhost:8081/login/oauth2/code/keycloak` |
| Пользователь с подтверждённой почтой | `student` / `student-local-pass` (`student@learning.local`) |
| Пользователь без подтверждения почты | `unverified` / `unverified-local-pass` (`unverified@learning.local`) |

Регистрацию клиента в `application.yaml` задают явными адресами провайдера (подсказка — в самом
файле), без `issuer-uri`: с ним сервис на старте обращается к Keycloak и без него не запускается.

### Kafka — глава 6

```bash
cd service-spring
docker compose --profile kafka up -d     # база и брокер; Keycloak главе 6 не нужен
docker exec -it spring_kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic lesson-completed --from-beginning --property print.key=true --property print.partition=true
```

Брокер — одна нода `apache/kafka:4.2.2` в режиме KRaft, память ограничена `KAFKA_HEAP_OPTS=-Xmx512m`.
Сервисы на машине ходят на `localhost:9092`, контейнеры сети Compose — на `kafka:29092`.
Стартер `spring-boot-starter-kafka` в `build.gradle.kts` закомментирован до урока 22. Тесты главы 6
поднимают свой брокер в Testcontainers (тот же образ), запущенный Compose им не нужен.

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
| 2 | `CatalogRoutesTest`, `CatalogPagingTest`, `ValidationTest`, `SchemaValidationTest` | остальные проверки в `ValidationTest` |
| 3 | `AccessRulesTest`, `AuthFlowTest`, `TokenCheckTest`, `RolesAndDraftsTest`, `ProgressTest`; урок 15 — `OAuthLoginTest` | refresh и logout, уроки черновика |
| 4 | — (тесты пишет студент в `src/test/kotlin/com/learning/spring/student/`) | — |
| 5 | `SlugGeneratorTest`, `ProgressReportServiceTest` | — |
| 6 | `LessonEventPublishingTest`, `AfterCommitTest`; в `achievements-spring/` — `AchievementConsumerTest`, `DeadLetterTest` (тест доставки пишет студент в `achievements-spring/src/test/kotlin/com/learning/achievements/student/`) | — |
| 7 | полный прогон ядра, `tests-07.txt` | — |

С `-Pchapter=6` и дальше `testReport` прогоняет и `achievements-spring` — отчёт один на оба сервиса.

Тесты ходят в сервис по HTTP и в базу по JDBC (PostgreSQL в Testcontainers, нужен Docker), поэтому
компилируются на чистом шаблоне. Маршруты — с префиксом `/api/v1`, как у Ktor-двойника; пробы — в корне.

## Что в каркасе

- `Application.kt` — точка входа; сканирование `@ConfigurationProperties` включено.
- `models/ApiModels.kt` — DTO и `ErrorResponse`, копия контракта Ktor-версии. Готово.
- `catalog/` — `CourseService`, `LessonService`, `CourseRepository` и его заглушка `InMemoryCourseRepository`
  без аннотаций; `ManualWiring` — ручная сборка (урок 2 её удаляет).
- `health/HealthStub.kt` — заглушка `/health` (урок 4 заменяет её Actuator).
- `auth/ExternalLoginService.kt` — заглушка привязки учётки провайдера и класс `ExternalIdentity` (урок 15).
- `events/LessonCompletedEvent.kt` — событие «урок пройден», контракт с `achievements-spring` (глава 6). Готово.
- `keycloak/realm-learning.json` — учебный realm для Keycloak (урок 15). Готово.
- `legacy/` (Java) — материал главы 5: `SlugGenerator`, `ProgressReportService`, записи `ProgressEntry` и `CourseProgress`.
- `application.yaml` — порт, база, Flyway и закомментированные места под настройки уроков 3, 4, 5, 8.

## Как подключить модуль к существующему `backend-<логин>`

Модуль самодостаточен: версии плагинов Spring заданы в `service-spring/build.gradle.kts`, корневой
`build.gradle.kts` менять не нужно.

```bash
git remote add template https://github.com/AI-Sprints-School/backend-template.git   # если ещё нет
git fetch template
git checkout template/main -- service-spring achievements-spring .github/workflows/service-spring.yml
```

В `settings.gradle.kts` своего репозитория добавьте **отдельной строкой** (файл из шаблона не копируйте — он затрёт
ваши `include`):

```kotlin
include("service-spring")
include("achievements-spring")
```

Закоммитьте в `main`: `git add service-spring achievements-spring .github settings.gradle.kts && git commit -m "Подключить service-spring"`.
Ветки сдачи — `spring-NN`.
