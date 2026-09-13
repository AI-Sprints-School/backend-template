# backend-template — шаблон студента бэкенд-трека AI Sprints

Шаблон для двух курсов школы AI Sprints: **«Корутины и Flow на сервере»**
(модуль `coroutines/`) и **«REST-сервис на Ktor: API, база, авторизация,
тесты»** (модуль `service/`). В шаблоне уже есть сборка, база, миграции,
спецификация API, готовый CI и **все тесты**. Тела функций, которые пишет
студент, — `TODO("Глава N, урок M: …")`; тесты на старте красные. Задача по
курсу — сделать их зелёными, урок за уроком.

## Как начать

1. Нажмите **Use this template → Create a new repository** на странице
   этого репозитория. Имя своего репозитория — `backend-<ваш логин>`,
   видимость — публичная: ИИ-ментор платформы читает только публичные
   pull request.
2. Клонируйте свой репозиторий:

   ```bash
   git clone git@github.com:<ваш-логин>/backend-<ваш-логин>.git
   cd backend-<ваш-логин>
   ```

3. Первая команда сборки — по курсу:

   ```bash
   ./gradlew :coroutines:testReport -Plesson=01   # курс корутин: тесты урока 1
   ./gradlew :service:test                        # курс REST-сервиса: все тесты
   ```

   Что увидите: проект собирается, тесты запускаются и падают — это
   ожидаемо, реализации ещё нет. Отчёт первого прогона появится в
   `coroutines/reports/tests-01.txt`.

Запуск сервиса курса «REST-сервис на Ktor» (нужен Docker):

```bash
cd service
cp env.example .env
docker compose up -d postgres redis   # база и Redis
cd ..
./gradlew :service:run                 # сервис на http://localhost:8080
curl -i http://localhost:8080/health   # пока маршрут не написан — 500
```

Курс корутин запускает задачи модуля из IDE через Run Anything:
`gradle :coroutines:runLesson -Plesson=01` и `gradle :coroutines:testReport -Plesson=01`.

## Структура

| Путь | Что там |
|---|---|
| `coroutines/` | модуль курса «Корутины и Flow на сервере»: имитация загрузчика, заготовки уроков 1–15, тесты |
| `coroutines/reports/` | отчёты прогона по урокам `tests-NN.txt` и логи `run-NN.txt` — коммитятся в pull request |
| `.github/workflows/coroutines.yml` | CI модуля корутин: тесты урока из ветки `lesson-NN`, сверка отчёта |
| `service/` | модуль курса «REST-сервис на Ktor»: сервис на Ktor + Exposed + PostgreSQL |
| `service/src/main/resources/db/migration` | схема базы — миграции Flyway |
| `service/src/test` | тесты: юнит, HTTP через Ktor Test, база через Testcontainers |
| `service/docs/` | спецификация API и схема базы |
| `service/reports/` | отчёты прогона по главам `tests-0N.txt` — коммитятся в pull request |
| `service/scripts/perf/` | большой набор данных для главы 7 |
| `defects/` | снимки сервиса с дефектами для главы 8; в шаблоне пусто, выдаются позже |
| `.github/workflows/service.yml` | CI сервиса: прогон тестов главы, сверка отчёта, ядро глав 1–N зелёное |

Workflow разнесены по модулям: изменения в `coroutines/**` не запускают CI
сервиса, и наоборот.

## Как сдавать

1. На каждый урок — ветка `lesson-NN` от `main` (`lesson-01`, `lesson-02`, …).
2. Сделайте тесты урока зелёными и сгенерируйте отчёт прогона:

   ```bash
   ./gradlew :coroutines:testReport -Plesson=NN    # курс корутин → coroutines/reports/tests-NN.txt
   ./gradlew :service:testReport -Pchapter=N       # курс REST-сервиса → service/reports/tests-0N.txt
   ```

   Отчёт правится только прогоном: CI перегенерирует его и сверяет с
   закоммиченным.
3. Закоммитьте код и отчёт, откройте pull request `lesson-NN → main` в своём
   репозитории. CI прогонит тесты; ссылку на pull request отправьте в задаче
   урока на платформе.

Теги тестов сервиса: `core` — обязательное ядро, `extension` — расширение,
`star` — звёздочка, `chapterN` — глава, к концу которой тест зеленеет.
Отчёт показывает ядро глав 1–N; красные тесты следующих глав — норма.

Куда идти за помощью:

- **ИИ-ментор** — в задаче урока на платформе: читает ваш pull request,
  отвечает на вопросы по коду и тестам.
- **Ментор-человек** — Иван Ветров, основатель школы, Telegram
  [@ievetrov42](https://t.me/ievetrov42): на связи на всём протяжении курса.

## Требования

| Инструмент | Версия |
|---|---|
| JDK | Temurin 25 LTS (машина только с JDK 24: `-PjavaToolchain=24`) |
| Kotlin | 2.4.20 |
| Gradle | 9.7.1 (wrapper в репозитории) |
| Ktor | 3.5.2 |
| Exposed | 1.5.0 |
| kotlinx.coroutines | 1.11.0 |
| PostgreSQL | 18 (в Docker) |
| Docker | нужен для `service/`: Compose и Testcontainers |

## Лицензия

MIT — см. [`LICENSE`](LICENSE). Код из шаблона и своё решение можно
показывать в портфолио.
