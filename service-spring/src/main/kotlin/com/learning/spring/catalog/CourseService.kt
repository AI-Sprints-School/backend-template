package com.learning.spring.catalog

import com.learning.spring.models.CourseResponse

/** Сервис каталога курсов. Не бин: стереотип и конструктор ставит урок 2. */
class CourseService(private val courseRepository: CourseRepository) {
    fun listPublished(): List<CourseResponse> =
        TODO("Глава 2, урок 6: опубликованные курсы через репозиторий")

    fun getById(id: String): CourseResponse =
        TODO("Глава 2, урок 6: курс по id или 404")
}
