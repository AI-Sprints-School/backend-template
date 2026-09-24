package com.learning.services

import com.learning.domain.models.Course
import com.learning.domain.models.Roles
import com.learning.domain.models.Viewer
import com.learning.models.*
import com.learning.repositories.CourseRepository
import com.learning.repositories.LessonRepository
import java.math.BigDecimal
import java.util.*

class CourseService(
    private val courseRepository: CourseRepository,
    private val lessonRepository: LessonRepository,
) {
    
    fun getAllCourses(): List<CourseResponse> {
        TODO("Глава 4, урок 15: CourseService.getAllCourses")
    }
    
    /**
     * Курс по id. Черновик отдаётся только тому, кому [canSeeDraft] разрешает его видеть;
     * остальным — `null`, как будто курса нет: маршрут ответит 404, а не 403,
     * и не раскроет, что такой курс существует.
     */
    fun getCourseById(id: String, viewer: Viewer? = null): CourseResponse? {
        TODO("Глава 4, урок 15: CourseService.getCourseById")
    }

    /**
     * Может ли [viewer] видеть черновик [course]. До урока 23 — никто: пользователя ещё нет.
     * Глава 5, урок 23: черновик видят автор курса (`course.authorId`) и роль admin.
     */
    fun canSeeDraft(course: Course, viewer: Viewer?): Boolean = false
    
    fun getCourseLessons(courseId: String): List<LessonResponse> {
        TODO("Глава 4, урок 15: CourseService.getCourseLessons")
    }
    
    fun createCourse(request: CourseRequest, authorId: UUID? = null): CourseResponse {
        TODO("Глава 4, урок 15: CourseService.createCourse")
    }
    
    fun updateCourse(id: String, request: CourseRequest): CourseResponse {
        TODO("Глава 4, урок 15: CourseService.updateCourse")
    }
    
    fun deleteCourse(id: String): Boolean {
        TODO("Глава 4, урок 15: CourseService.deleteCourse")
    }
    
    // Extension functions for mapping domain models to API responses
    private fun com.learning.domain.models.Course.toCourseResponse(): CourseResponse {
        return CourseResponse(
            id = this.id.toString(),
            title = this.title,
            description = this.description,
            coverImage = this.thumbnailUrl,
            category = "PROGRAMMING", // Default category, can be enhanced later
            difficulty = this.difficulty.uppercase(),
            duration = this.duration,
            lessonsCount = this.lessonsCount,
            rating = 4.5, // Default rating, can be calculated from reviews
            studentsCount = 0, // Can be calculated from enrollments
            price = this.price.toDouble(),
            isPremium = this.price > BigDecimal.ZERO,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun com.learning.domain.models.Lesson.toLessonResponse(): LessonResponse {
        return LessonResponse(
            id = this.id.toString(),
            courseId = this.courseId.toString(),
            title = this.title,
            description = this.description,
            orderIndex = this.order,
            duration = this.duration,
            videoUrl = this.videoUrl,
            isPreview = false, // Can be determined by business logic
            contents = emptyList() // Can be populated from lesson contents
        )
    }
}