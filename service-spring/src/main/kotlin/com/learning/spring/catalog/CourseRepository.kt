package com.learning.spring.catalog

import com.learning.spring.models.CourseResponse

/**
 * Доступ к курсам. Каркас: реализация в памяти без аннотаций (урок 2 делает её бином),
 * в главе 2 её заменит репозиторий Spring Data над базой (урок 9).
 */
interface CourseRepository {
    fun findPublished(): List<CourseResponse>
    fun findById(id: String): CourseResponse?
}
