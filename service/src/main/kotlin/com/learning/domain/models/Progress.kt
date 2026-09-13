package com.learning.domain.models

import java.time.Instant
import java.util.*

/** Строка таблицы `user_progress`. */
data class ProgressRecord(
    val id: UUID,
    val userId: UUID,
    val courseId: UUID?,
    val lessonId: UUID?,
    val quizId: UUID?,
    val type: String,
    val isCompleted: Boolean,
    val score: Int? = null,
    val completedAt: Instant? = null,
)

/** Значения колонки `progress_type`. */
object ProgressType {
    const val LESSON = "lesson"
    const val COURSE = "course"
    const val QUIZ = "quiz"
}

/** Сколько уроков курса завершено из скольких. */
data class CourseCompletion(val completed: Int, val total: Int) {
    val percentage: Int get() = if (total == 0) 0 else completed * 100 / total
    val isCompleted: Boolean get() = total > 0 && completed >= total
}
