package com.learning.repositories

import com.learning.database.Courses
import com.learning.database.Lessons
import com.learning.domain.models.Lesson
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class LessonRepository {
    private val logger = LoggerFactory.getLogger(LessonRepository::class.java)

    fun createLesson(
        courseId: UUID,
        title: String,
        description: String?,
        content: String,
        videoUrl: String?,
        duration: Int,
        order: Int,
        isPublished: Boolean = false
    ): Lesson? {
        TODO("Глава 3, урок 13: LessonRepository.createLesson")
    }

    fun findById(lessonId: UUID): Lesson? {
        TODO("Глава 3, урок 13: LessonRepository.findById")
    }

    fun findAll(): List<Lesson> {
        TODO("Глава 3, урок 13: LessonRepository.findAll")
    }

    fun findByCourseId(courseId: UUID): List<Lesson> {
        TODO("Глава 3, урок 13: LessonRepository.findByCourseId")
    }

    fun findPublishedByCourseId(courseId: UUID): List<Lesson> {
        TODO("Глава 3, урок 13: LessonRepository.findPublishedByCourseId")
    }

    fun updateLesson(
        lessonId: UUID,
        title: String? = null,
        description: String? = null,
        content: String? = null,
        videoUrl: String? = null,
        duration: Int? = null,
        order: Int? = null,
        isPublished: Boolean? = null
    ): Boolean {
        TODO("Глава 3, урок 13: LessonRepository.updateLesson")
    }

    fun deleteLesson(lessonId: UUID): Boolean {
        TODO("Глава 3, урок 13: LessonRepository.deleteLesson")
    }

    fun reorderLessons(courseId: UUID, lessonOrders: Map<UUID, Int>): Boolean {
        TODO("Глава 3, урок 13: LessonRepository.reorderLessons")
    }

    fun getNextLesson(courseId: UUID, currentOrder: Int): Lesson? {
        TODO("Глава 3, урок 13: LessonRepository.getNextLesson")
    }

    fun getPreviousLesson(courseId: UUID, currentOrder: Int): Lesson? {
        TODO("Глава 3, урок 13: LessonRepository.getPreviousLesson")
    }

    fun getTotalDuration(courseId: UUID): Int {
        TODO("Глава 3, урок 13: LessonRepository.getTotalDuration")
    }

    /** `courses.lessons_count` = число уроков курса. Вызывается внутри транзакции записи. */
    private fun refreshLessonsCount(courseId: UUID) {
        TODO("Глава 3, урок 13: LessonRepository.refreshLessonsCount")
    }

    private fun ResultRow.toLesson(): Lesson {
        return Lesson(
            id = this[Lessons.id],
            courseId = this[Lessons.courseId],
            title = this[Lessons.title],
            description = this[Lessons.description],
            content = this[Lessons.content],
            videoUrl = this[Lessons.videoUrl],
            duration = this[Lessons.duration],
            order = this[Lessons.order],
            isPublished = this[Lessons.isPublished],
            createdAt = this[Lessons.createdAt],
            updatedAt = this[Lessons.updatedAt]
        )
    }
}
