package com.learning.services

import com.learning.domain.models.CourseCompletion
import com.learning.domain.models.Lesson
import com.learning.domain.models.ProgressRecord
import com.learning.domain.models.ProgressType
import com.learning.domain.models.User
import com.learning.models.UpdateUserProfileRequest
import com.learning.repositories.LessonRepository
import com.learning.repositories.ProgressRepository
import com.learning.repositories.UserRepository
import com.learning.utils.AppException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

/**
 * Unit тесты UserService: профиль и прогресс на моках репозиториев.
 */
@Tag("core")
@Tag("chapter5")
class UserServiceTest {

    private lateinit var users: UserRepository
    private lateinit var progress: ProgressRepository
    private lateinit var lessons: LessonRepository
    private lateinit var service: UserService

    private val now = Instant.parse("2026-09-13T12:00:00Z")
    private val user = User(
        id = UUID.randomUUID(),
        email = "ivan@example.com",
        passwordHash = "hash",
        firstName = "Иван",
        lastName = "Иванов",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
    )

    @BeforeEach
    fun setUp() {
        users = mockk(relaxed = true)
        progress = mockk(relaxed = true)
        lessons = mockk(relaxed = true)
        service = UserService(users, progress, lessons, Clock.fixed(now, ZoneOffset.UTC))
        every { users.findById(user.id) } returns user
    }

    // ==================== Профиль ====================

    @Test
    fun `getUserProfile should return real user data`() {
        every { progress.findByUser(user.id) } returns emptyList()

        val profile = service.getUserProfile(user.id.toString())

        assertEquals(user.id.toString(), profile.id)
        assertEquals("ivan@example.com", profile.email)
        assertEquals("Иван", profile.firstName)
        assertEquals("student", profile.role)
        assertEquals(0, profile.experience)
        assertEquals("BEGINNER", profile.level)
        assertEquals(user.createdAt.toEpochMilli(), profile.joinDate)
    }

    @Test
    fun `getUserProfile should throw NotFound when user missing`() {
        val missing = UUID.randomUUID()
        every { users.findById(missing) } returns null

        assertThrows<AppException.NotFoundError> { service.getUserProfile(missing.toString()) }
    }

    @Test
    fun `getUserProfile should reject invalid id with BadRequest`() {
        assertThrows<AppException.BadRequestError> { service.getUserProfile("not-a-uuid") }
    }

    @Test
    fun `updateUserProfile should update names and return updated profile`() {
        val updated = user.copy(firstName = "Пётр", lastName = "Сидоров")
        every { users.updateProfile(user.id, "Пётр", "Сидоров", null) } returns true
        every { users.findById(user.id) } returnsMany listOf(user, updated)

        val profile = service.updateUserProfile(user.id.toString(), UpdateUserProfileRequest("Пётр", "Сидоров", null, null))

        assertEquals("Пётр", profile.firstName)
        assertEquals("Сидоров", profile.lastName)
        verify { users.updateProfile(user.id, "Пётр", "Сидоров", null) }
    }

    @Test
    fun `updateUserProfile should reject email taken by another user`() {
        every { users.findByEmail("taken@example.com") } returns user.copy(id = UUID.randomUUID(), email = "taken@example.com")

        assertThrows<AppException.ConflictError> {
            service.updateUserProfile(user.id.toString(), UpdateUserProfileRequest(null, null, "taken@example.com", null))
        }
        verify(exactly = 0) { users.updateProfile(any(), any(), any(), any()) }
    }

    @Test
    fun `updateUserProfile should not treat own email as conflict`() {
        every { users.findByEmail(user.email) } returns user

        service.updateUserProfile(user.id.toString(), UpdateUserProfileRequest(null, null, user.email, null))

        verify(exactly = 0) { users.updateProfile(any(), any(), any(), any()) }
    }

    // ==================== Прогресс ====================

    @Test
    fun `getUserProgress should return zeros for user without progress`() {
        every { progress.findByUser(user.id) } returns emptyList()

        val result = service.getUserProgress(user.id.toString())

        assertEquals(0, result.totalCourses)
        assertEquals(0, result.completedLessons)
        assertEquals(0.0, result.averageScore)
        assertEquals(0, result.currentStreak)
        assertTrue(result.achievements.isEmpty())
    }

    @Test
    fun `getUserProgress should aggregate courses lessons and quizzes`() {
        val courseA = UUID.randomUUID()
        val courseB = UUID.randomUUID()
        val a1 = lesson(courseA, 10)
        val a2 = lesson(courseA, 20)
        val b1 = lesson(courseB, 30)
        every { lessons.findByCourseId(courseA) } returns listOf(a1, a2)
        every { lessons.findByCourseId(courseB) } returns listOf(b1)
        every { progress.findByUser(user.id) } returns listOf(
            record(ProgressType.COURSE, courseA, completed = true),
            record(ProgressType.COURSE, courseB, completed = false),
            record(ProgressType.LESSON, courseA, a1.id),
            record(ProgressType.LESSON, courseA, a2.id),
            record(ProgressType.QUIZ, courseA, completed = true, score = 80),
            record(ProgressType.QUIZ, courseA, completed = false, score = 60),
        )

        val result = service.getUserProgress(user.id.toString())

        assertEquals(2, result.totalCourses)
        assertEquals(1, result.completedCourses)
        assertEquals(1, result.inProgressCourses)
        assertEquals(3, result.totalLessons)
        assertEquals(2, result.completedLessons)
        assertEquals(2, result.totalTests)
        assertEquals(1, result.completedTests)
        assertEquals(70.0, result.averageScore)
        assertEquals(30, result.totalStudyTime)
        assertTrue(UserService.ACHIEVEMENT_FIRST_COURSE in result.achievements)
    }

    @Test
    fun `getUserProgress should compute current and longest streak`() {
        val course = UUID.randomUUID()
        val days = listOf("2026-09-01", "2026-09-02", "2026-09-03", "2026-09-12", "2026-09-13")
        every { progress.findByUser(user.id) } returns days.map {
            record(ProgressType.LESSON, course, UUID.randomUUID(), at = Instant.parse("${it}T10:00:00Z"))
        }

        val result = service.getUserProgress(user.id.toString())

        assertEquals(2, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `completeLesson should throw NotFound for unknown lesson`() {
        val lessonId = UUID.randomUUID()
        every { lessons.findById(lessonId) } returns null

        assertThrows<AppException.NotFoundError> { service.completeLesson(user.id.toString(), lessonId.toString()) }
        verify(exactly = 0) { progress.markLessonCompleted(any(), any(), any()) }
    }

    @Test
    fun `completeLesson should report percentage for partially completed course`() {
        val course = UUID.randomUUID()
        val all = listOf(lesson(course, 10), lesson(course, 10), lesson(course, 10))
        every { lessons.findById(all[0].id) } returns all[0]
        every { progress.courseCompletion(user.id, course) } returns CourseCompletion(completed = 1, total = 3)

        val result = service.completeLesson(user.id.toString(), all[0].id.toString())

        assertEquals(33, result.progress.courseCompletionPercentage)
        assertFalse(result.progress.courseCompleted)
        verify { progress.markLessonCompleted(user.id, course, all[0].id) }
        verify { progress.upsertCourseProgress(user.id, course, false) }
    }

    @Test
    fun `completeLesson should mark course completed when all lessons done`() {
        val course = UUID.randomUUID()
        val all = listOf(lesson(course, 10), lesson(course, 10))
        every { lessons.findById(all[1].id) } returns all[1]
        every { progress.courseCompletion(user.id, course) } returns CourseCompletion(completed = 2, total = 2)

        val result = service.completeLesson(user.id.toString(), all[1].id.toString())

        assertEquals(100, result.progress.courseCompletionPercentage)
        assertTrue(result.progress.courseCompleted)
        verify { progress.upsertCourseProgress(user.id, course, true) }
    }

    @Test
    fun `levelFor should grow with experience`() {
        assertEquals("BEGINNER", UserService.levelFor(0))
        assertEquals("INTERMEDIATE", UserService.levelFor(100))
        assertEquals("ADVANCED", UserService.levelFor(500))
    }

    private fun lesson(courseId: UUID, duration: Int) = Lesson(
        id = UUID.randomUUID(), courseId = courseId, title = "Урок", description = null,
        content = "Текст", videoUrl = null, duration = duration, order = 1,
    )

    private fun record(
        type: String,
        courseId: UUID,
        lessonId: UUID? = null,
        completed: Boolean = true,
        score: Int? = null,
        at: Instant = now,
    ) = ProgressRecord(
        id = UUID.randomUUID(), userId = user.id, courseId = courseId, lessonId = lessonId, quizId = null,
        type = type, isCompleted = completed, score = score, completedAt = if (completed) at else null,
    )
}
