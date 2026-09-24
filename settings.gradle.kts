rootProject.name = "backend-course"

// Сервис платформы обучения — курс «REST-сервис на Ktor»
include("service")

// Модуль курса «Корутины и Flow на сервере»
include("coroutines")

// Глава 4 курса «Карьерная подготовка бэкенд-разработчика» — код лежит
// в career/algorithms/, а не в career/ целиком: рядом в career/ живут
// файлы без кода (search-log.md, vacancies.md, mock/…) других глав курса.
include("career")
project(":career").projectDir = file("career/algorithms")
