package com.learning.repositories

import com.learning.database.Courses
import com.learning.domain.models.Course
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class CourseRepository {
    private val logger = LoggerFactory.getLogger(CourseRepository::class.java)

    fun createCourse(
        title: String,
        description: String,
        shortDescription: String?,
        thumbnailUrl: String?,
        price: BigDecimal,
        originalPrice: BigDecimal?,
        duration: Int,
        difficulty: String,
        isPublished: Boolean = false
    ): Course? {
        TODO("Глава 3, урок 12: CourseRepository.createCourse")
    }

    fun findById(courseId: UUID): Course? {
        TODO("Глава 3, урок 12: CourseRepository.findById")
    }

    fun findAll(): List<Course> {
        TODO("Глава 3, урок 12: CourseRepository.findAll")
    }

    fun findPublished(): List<Course> {
        TODO("Глава 3, урок 12: CourseRepository.findPublished")
    }

    fun findByDifficulty(difficulty: String): List<Course> {
        TODO("Глава 3, урок 12: CourseRepository.findByDifficulty")
    }

    fun updateCourse(
        courseId: UUID,
        title: String? = null,
        description: String? = null,
        shortDescription: String? = null,
        thumbnailUrl: String? = null,
        price: BigDecimal? = null,
        originalPrice: BigDecimal? = null,
        duration: Int? = null,
        difficulty: String? = null,
        isPublished: Boolean? = null
    ): Boolean {
        TODO("Глава 3, урок 12: CourseRepository.updateCourse")
    }

    fun deleteCourse(courseId: UUID): Boolean {
        TODO("Глава 3, урок 12: CourseRepository.deleteCourse")
    }

    fun updateLessonsCount(courseId: UUID, count: Int): Boolean {
        TODO("Глава 3, урок 12: CourseRepository.updateLessonsCount")
    }

    fun searchCourses(query: String): List<Course> {
        TODO("Глава 3, урок 12: CourseRepository.searchCourses")
    }

    fun getCoursesWithDiscount(): List<Course> {
        TODO("Глава 3, урок 12: CourseRepository.getCoursesWithDiscount")
    }

    private fun ResultRow.toCourse(): Course {
        return Course(
            id = this[Courses.id],
            title = this[Courses.title],
            description = this[Courses.description],
            shortDescription = this[Courses.shortDescription],
            thumbnailUrl = this[Courses.thumbnailUrl],
            price = this[Courses.price],
            originalPrice = this[Courses.originalPrice],
            duration = this[Courses.duration],
            difficulty = this[Courses.difficulty],
            isPublished = this[Courses.isPublished],
            lessonsCount = this[Courses.lessonsCount],
            createdAt = this[Courses.createdAt],
            updatedAt = this[Courses.updatedAt]
        )
    }
}
