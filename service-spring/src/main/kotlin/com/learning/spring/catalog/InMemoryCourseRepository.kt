package com.learning.spring.catalog

import com.learning.spring.models.CourseResponse

/** Заглушка доступа к данным: курсы в памяти. Не бин: стереотип ставит урок 2. */
class InMemoryCourseRepository : CourseRepository {
    override fun findPublished(): List<CourseResponse> =
        TODO("Глава 1, урок 2: сделать репозиторий бином; тело — в главе 2, урок 6")

    override fun findById(id: String): CourseResponse? =
        TODO("Глава 1, урок 2: сделать репозиторий бином; тело — в главе 2, урок 6")
}
