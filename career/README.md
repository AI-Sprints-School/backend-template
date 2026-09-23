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

## Файлы глав

| Урок | Файл, который вы создаёте | Шаблон |
|---|---|---|
| 1 | `career/search-log.md` — выборка вакансий за неделю | `templates/search-log.md` |
| 2 | `career/vacancies.md` — пять вакансий построчно | `templates/vacancies.md` |
| 3 | `career/funnel-map.md` — карта воронки | `templates/funnel-map.md` |

Файл урока создаётся копией шаблона: `cp career/templates/search-log.md career/search-log.md`.
Шаблоны не правьте — они остаются образцом.

## Личные данные

Телефон, почту и ФИО в этот публичный репозиторий не пишите. Резюме живёт в
Google Docs, отклики — в панели задач платформы.
