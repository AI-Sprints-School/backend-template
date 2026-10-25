package com.learning.spring.catalog

/**
 * Ручная сборка сервисов каталога — как модуль `dependencies {}` в Ktor.
 * Урок 2: сервисы становятся бинами, этот файл удаляется.
 */
object ManualWiring {
    private val repository: CourseRepository = InMemoryCourseRepository()

    val courseService: CourseService = CourseService(repository)
    val lessonService: LessonService = LessonService(repository)
}
