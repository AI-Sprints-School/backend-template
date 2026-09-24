# career/ — курс «Карьерная подготовка бэкенд-разработчика»

Накопительная папка курса. Каждое задание кладёт сюда свой файл, следующие
задания читают его из `main`. После приёмки pull request вливайте его в
`main` сами.

## Как папка попала в ваш репозиторий

Один раз, в уроке 1, из `main` вашего `backend-<логин>`:

```bash
git remote add template https://github.com/AI-Sprints-School/backend-template.git
git fetch template
git checkout template/main -- career
git commit -m "career: заготовка курса"
git push
```

## Как сдавать

- ветка на урок — `career-NN` от свежего `main` (`career-01`, `career-02`, …);
- в pull request — только файлы урока; заготовки из этой папки лежат в `main`
  и в изменения не попадают;
- заголовок pull request начинается с кода задачи: `BE-CAR-1-1`, `BE-CAR-2-1`, …;
- репозиторий публичный — иначе проверяющий не прочитает изменения.

## Файлы глав курса «Карьерная подготовка бэкенд-разработчика»

| Урок | Файл, который вы создаёте | Шаблон |
|---|---|---|
| 1 | `career/search-log.md` — выборка вакансий за неделю | `templates/search-log.md` |
| 2 | `career/vacancies.md` — пять вакансий построчно | `templates/vacancies.md` |
| 3 | `career/funnel-map.md` — карта воронки | `templates/funnel-map.md` |
| 4 | резюме — в Google Docs, не в репозитории | `templates/resume-structure.md` (только структура, текст копируется в документ) |
| 8 | `career/interview-map.md` — 15 вопросов секции «платформа» | `templates/interview-map.md` |
| 9 | `career/review-java-core.md` — ревью `fixtures/lesson-09-java-core/CustomerCard.java` | шаблон — в тексте задания урока |
| 10 | `career/review-concurrency.md` — ревью `fixtures/lesson-10-concurrency/AccountCache.java` | шаблон — в тексте задания урока |
| 11 | `career/spring-answers.md` — разбор `fixtures/lesson-11-spring/` | шаблон — в тексте задания урока |
| 12 | `career/sql-http.md` | шаблон — в тексте задания урока |
| 13 | `career/algorithms/src/main/kotlin/algorithms/l13/Complexity.kt`, `career/reports/algorithms-13.txt` | заготовка уже в модуле |
| 14 | `career/algorithms/src/main/kotlin/algorithms/l14/HashAndWindow.kt`, `career/reports/algorithms-14.txt` | заготовка уже в модуле |
| 15 | `career/algorithms/src/main/kotlin/algorithms/l15/IntervalsAndSearch.kt`, `career/reports/algorithms-15.txt` | заготовка уже в модуле |
| 16 | `career/algorithms/src/main/kotlin/algorithms/l16/Graphs.kt`, `career/mock/live-coding-16.md` | заготовка уже в модуле; банк — `career/mock/live-coding-bank.md`; сценарий записи — `templates/live-coding-16.md` |
| 18 | `career/test-task/README.md`, `tests-report.txt`, `time-log.md` (код — в отдельном репозитории) | спецификация тестового — в тексте задания урока, файла в репозитории нет |
| 19 | `career/test-task/design.md` | шаблон — в тексте задания урока |

Файл урока создаётся копией шаблона, если он лежит в `templates/`:
`cp career/templates/search-log.md career/search-log.md`. Шаблоны не
правьте — они остаются образцом. Часть заданий (9–12, 18, 19) даёт свой
шаблон прямо в тексте задания — для них отдельного файла в `templates/`
нет, это осознанно: лишний файл в репозитории ничем не помогает уроку.

## Файлы курса «Карьерное сопровождение выпускника»

| Урок | Файл, который вы создаёте | Шаблон |
|---|---|---|
| 8 | `career/next-skill.md` — выбор следующего навыка по вакансиям 1–3 года | `templates/next-skill.md` |

Курс опирается на файлы `career/vacancies.md`, `career/funnel-map.md`,
`career/interview-map.md`, `career/algorithms/` — они уже в `main` из
курса «Карьерная подготовка бэкенд-разработчика».

## Глава 4: `career/algorithms/` — модуль с кодом

В отличие от файлов глав 1–3, глава 4 (уроки 13–16) — это Gradle-модуль.
Он подключён под именем `:career` (`settings.gradle.kts`), физически лежит
в `career/algorithms/` — рядом, прямо в `career/`, живут файлы без кода
остальных глав. Отчёты прогона пишутся в `career/reports/`, а не внутрь
`career/algorithms/`.

```bash
./gradlew :career:test                      # все тесты главы 4
./gradlew :career:testReport -Plesson=13     # тесты урока 13, career/reports/algorithms-13.txt
```

На старте в `career/algorithms/src/main/kotlin/algorithms/l1N/…` — тела
некоторых функций `TODO(...)`, тесты по ним красные; часть функций уже
работает, и в неё встроена задача урока 13 — переписать `hasDuplicatesSlow`
в `hasDuplicates` за O(n) вместо скрытого O(n²) через `contains()` в цикле.
Отчёт коммитится файлом в `career/reports/algorithms-NN.txt` — так же, как
`service/reports/tests-0N.txt` у курса про Ktor.

Урок 16 добавляет запись самопрогона: `career/mock/live-coding-bank.md` —
банк задач для тренировки (одна из них, кроме BFS/DFS-задачи модуля,
решается вслух под запись), `career/mock/live-coding-16.md` — файл, который
создаёт студент с выдержками расшифровки.

## Личные данные

Телефон, почту и ФИО в этот публичный репозиторий не пишите. Резюме живёт в
Google Docs, отклики — в панели задач платформы.
