package com.learning.repositories

import com.learning.database.Lessons
import com.learning.database.UserProgress
import com.learning.domain.models.CourseCompletion
import com.learning.domain.models.ProgressRecord
import com.learning.domain.models.ProgressType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Прогресс пользователя — таблица `user_progress`.
 *
 * В одной таблице лежат три вида строк (`progress_type`): урок, курс, квиз.
 * Урок и курс засчитываются один раз — это держат частичные уникальные индексы
 * миграции V3, а репозиторий пишет через `INSERT … ON CONFLICT DO NOTHING`.
 */
class ProgressRepository {
    private val logger = LoggerFactory.getLogger(ProgressRepository::class.java)

    /**
     * Засчитывает урок. Повторная отметка ничего не меняет.
     * @return true — отметка новая, false — урок уже был засчитан.
     */
    fun markLessonCompleted(userId: UUID, courseId: UUID, lessonId: UUID): Boolean = TODO("Глава 3, урок 14: ProgressRepository.markLessonCompleted")

    /** Строка курса: создаётся при первом засчитанном уроке, флаг завершения обновляется. */
    fun upsertCourseProgress(userId: UUID, courseId: UUID, completed: Boolean): Unit = TODO("Глава 3, урок 14: ProgressRepository.upsertCourseProgress")

    /** Сколько уроков курса пользователь завершил. */
    fun countCompletedLessons(userId: UUID, courseId: UUID): Int = TODO("Глава 3, урок 14: ProgressRepository.countCompletedLessons")

    /**
     * Прогресс по курсу одним запросом: уроки курса LEFT JOIN отметки пользователя,
     * агрегаты COUNT по урокам и по отметкам.
     */
    fun courseCompletion(userId: UUID, courseId: UUID): CourseCompletion = TODO("Глава 3, урок 14: ProgressRepository.courseCompletion")

    /** Все строки прогресса пользователя. */
    fun findByUser(userId: UUID): List<ProgressRecord> = TODO("Глава 3, урок 14: ProgressRepository.findByUser")

    private fun ResultRow.toRecord() = ProgressRecord(
        id = this[UserProgress.id],
        userId = this[UserProgress.userId],
        courseId = this[UserProgress.courseId],
        lessonId = this[UserProgress.lessonId],
        quizId = this[UserProgress.quizId],
        type = this[UserProgress.progressType],
        isCompleted = this[UserProgress.isCompleted],
        score = this[UserProgress.score],
        completedAt = this[UserProgress.completedAt],
    )
}
