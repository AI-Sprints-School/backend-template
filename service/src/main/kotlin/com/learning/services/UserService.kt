package com.learning.services

import com.learning.domain.models.ProgressRecord
import com.learning.domain.models.ProgressType
import com.learning.domain.models.User
import com.learning.models.*
import com.learning.repositories.LessonRepository
import com.learning.repositories.ProgressRepository
import com.learning.repositories.UserRepository
import com.learning.utils.AppException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

/**
 * Профиль и прогресс пользователя.
 *
 * Контракт — `docs/API_SPECIFICATION.md` (3.3.2, 3.9) и тесты
 * `UserServiceTest`, `ProgressRoutesTest`. Прогресс хранится в `user_progress`
 * (миграции V1, V3): урок засчитывается один раз, строка курса — одна.
 * Опыт: [XP_PER_LESSON] за урок, [XP_PER_COURSE] за курс; серия — подряд идущие
 * дни (UTC) с завершённой активностью, текущее время — из [clock].
 *
 * Подсчёт статистики ([statsOf], [streaks], [toProfile]) отдан готовым: он общий
 * для профиля (урок 22) и прогресса (урок 24) и не входит в тему главы 5.
 */
class UserService(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository,
    private val lessonRepository: LessonRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun getUserProfile(userId: String): UserProfileResponse {
        TODO("Глава 5, урок 22: профиль пользователя из базы")
    }

    fun updateUserProfile(userId: String, request: UpdateUserProfileRequest): UserProfileResponse {
        TODO("Глава 5, урок 22: обновление профиля; чужая почта — 409")
    }

    fun getUserProgress(userId: String): UserProgressResponse {
        TODO("Глава 5, урок 24: прогресс пользователя из user_progress")
    }

    /**
     * Отмечает урок завершённым и пересчитывает прогресс курса.
     * Повторная отметка не создаёт дублей и возвращает тот же процент.
     */
    fun completeLesson(userId: String, lessonId: String): LessonCompletionResponse {
        TODO("Глава 5, урок 24: отметить урок и пересчитать процент курса")
    }

    // ---------- Подсчёт ----------

    private data class Stats(
        val startedCourses: Int = 0,
        val completedCourses: Int = 0,
        val totalLessons: Int = 0,
        val completedLessons: Int = 0,
        val totalTests: Int = 0,
        val completedTests: Int = 0,
        val averageScore: Double = 0.0,
        val studyMinutes: Int = 0,
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val achievements: List<String> = emptyList(),
    ) {
        val experience: Int get() = completedLessons * XP_PER_LESSON + completedCourses * XP_PER_COURSE
    }

    private fun statsOf(userId: UUID): Stats {
        val rows = progressRepository.findByUser(userId)

        val courseRows = rows.filter { it.type == ProgressType.COURSE }
        val lessonRows = rows.filter { it.type == ProgressType.LESSON && it.isCompleted }
        val quizRows = rows.filter { it.type == ProgressType.QUIZ }

        val startedCourseIds = courseRows.mapNotNull { it.courseId }.toSet()
        val lessonsByCourse = startedCourseIds.associateWith { lessonRepository.findByCourseId(it) }
        val durationByLesson = lessonsByCourse.values.flatten().associate { it.id to it.duration }

        val scores = quizRows.mapNotNull { it.score }
        val (current, longest) = streaks(rows)
        val completedCourses = courseRows.count { it.isCompleted }

        return Stats(
            startedCourses = startedCourseIds.size,
            completedCourses = completedCourses,
            totalLessons = lessonsByCourse.values.sumOf { it.size },
            completedLessons = lessonRows.size,
            totalTests = quizRows.size,
            completedTests = quizRows.count { it.isCompleted },
            averageScore = if (scores.isEmpty()) 0.0 else scores.average(),
            studyMinutes = lessonRows.sumOf { row -> row.lessonId?.let { durationByLesson[it] } ?: 0 },
            currentStreak = current,
            longestStreak = longest,
            achievements = buildList {
                if (lessonRows.isNotEmpty()) add(ACHIEVEMENT_FIRST_LESSON)
                if (completedCourses > 0) add(ACHIEVEMENT_FIRST_COURSE)
                if (longest >= 7) add(ACHIEVEMENT_WEEK_STREAK)
            },
        )
    }

    /** Серия — подряд идущие дни (UTC) хотя бы с одной завершённой активностью. */
    private fun streaks(rows: List<ProgressRecord>): Pair<Int, Int> {
        val days = rows.filter { it.isCompleted }
            .mapNotNull { it.completedAt?.atZone(ZoneOffset.UTC)?.toLocalDate() }
            .toSortedSet()
        if (days.isEmpty()) return 0 to 0

        var longest = 1
        var run = 1
        days.zipWithNext().forEach { (prev, next) ->
            run = if (prev.plusDays(1) == next) run + 1 else 1
            longest = maxOf(longest, run)
        }

        val today = LocalDate.now(clock)
        var current = 0
        var day = if (today in days) today else today.minusDays(1)
        while (day in days) {
            current++
            day = day.minusDays(1)
        }
        return current to longest
    }

    private fun User.toProfile(stats: Stats) = UserProfileResponse(
        id = id.toString(),
        email = email,
        firstName = firstName,
        lastName = lastName,
        avatar = avatarUrl,
        level = levelFor(stats.experience),
        experience = stats.experience,
        completedCourses = stats.completedCourses,
        completedTests = stats.completedTests,
        currentStreak = stats.currentStreak,
        longestStreak = stats.longestStreak,
        joinDate = createdAt.toEpochMilli(),
        lastActivity = updatedAt.toEpochMilli(),
        role = role,
    )

    private fun findUser(userId: String): User {
        val uuid = parseUuid(userId, "Неверный ID пользователя")
        return userRepository.findById(uuid) ?: throw AppException.NotFoundError("Пользователь не найден")
    }

    private fun parseUuid(value: String, message: String): UUID =
        try {
            UUID.fromString(value)
        } catch (e: IllegalArgumentException) {
            throw AppException.BadRequestError(message)
        }

    companion object {
        const val XP_PER_LESSON = 10
        const val XP_PER_COURSE = 50
        const val ACHIEVEMENT_FIRST_LESSON = "Первый урок завершен"
        const val ACHIEVEMENT_FIRST_COURSE = "Первый курс завершен"
        const val ACHIEVEMENT_WEEK_STREAK = "Неделя подряд"

        fun levelFor(experience: Int): String = TODO("Глава 5, урок 22: уровень по опыту")
    }
}
