package com.learning.spring.catalog

import com.learning.spring.models.LessonResponse

/** Сервис уроков курса. Не бин: стереотип и конструктор ставит урок 2. */
class LessonService(private val courseRepository: CourseRepository) {
    fun listByCourse(courseId: String): List<LessonResponse> =
        TODO("Глава 2, урок 6: уроки курса по порядку")
}
