package com.learning.services

import com.learning.models.*
import com.learning.repositories.LessonRepository
import com.learning.repositories.ProgressRepository
import com.learning.repositories.UserRepository
import java.time.Clock

/**
 * Профиль и прогресс пользователя.
 *
 * Контракт — `docs/API_SPECIFICATION.md` (3.3.2, 3.9) и тесты
 * `UserServiceTest`, `ProgressRoutesTest`. Прогресс хранится в `user_progress`
 * (миграции V1, V3): урок засчитывается один раз, строка курса — одна.
 * Опыт: [XP_PER_LESSON] за урок, [XP_PER_COURSE] за курс; серия — подряд идущие
 * дни (UTC) с завершённой активностью, текущее время — из [clock].
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

    fun completeLesson(userId: String, lessonId: String): LessonCompletionResponse {
        TODO("Глава 5, урок 24: отметить урок и пересчитать процент курса")
    }

    companion object {
        const val XP_PER_LESSON = 10
        const val XP_PER_COURSE = 50
        const val ACHIEVEMENT_FIRST_LESSON = "Первый урок завершен"
        const val ACHIEVEMENT_FIRST_COURSE = "Первый курс завершен"
        const val ACHIEVEMENT_WEEK_STREAK = "Неделя подряд"

        /** BEGINNER до 100 опыта, INTERMEDIATE до 500, дальше ADVANCED. */
        fun levelFor(experience: Int): String = TODO("Глава 5, урок 24: уровень по опыту")
    }
}
