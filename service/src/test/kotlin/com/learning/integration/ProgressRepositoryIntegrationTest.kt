package com.learning.integration

import com.learning.TestData
import com.learning.domain.models.ProgressType
import com.learning.repositories.LessonRepository
import com.learning.repositories.ProgressRepository
import com.learning.repositories.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Интеграционные тесты ProgressRepository на PostgreSQL: схема — миграции Flyway,
 * включая уникальные частичные индексы V3.
 */
@Tag("core")
@Tag("chapter3")
class ProgressRepositoryIntegrationTest : DatabaseTestBase() {

    private val progress = ProgressRepository()
    private val users = UserRepository()

    private fun user(): UUID =
        users.createUser("u-${UUID.randomUUID()}@example.com", "hash", "Иван", "Иванов")!!.id

    @Test
    fun `markLessonCompleted should insert completed lesson row`() {
        val userId = user()
        val course = TestData.course()
        val lesson = TestData.lesson(course, 1)

        val inserted = progress.markLessonCompleted(userId, course.id, lesson.id)

        assertTrue(inserted)
        val row = progress.findByUser(userId).single()
        assertEquals(ProgressType.LESSON, row.type)
        assertEquals(lesson.id, row.lessonId)
        assertEquals(course.id, row.courseId)
        assertTrue(row.isCompleted)
        assertNotNull(row.completedAt)
    }

    @Test
    fun `findByUser should return empty list for user without progress`() {
        assertTrue(progress.findByUser(user()).isEmpty())
    }

    @Test
    fun `findByUser should return only rows of that user`() {
        val alice = user()
        val bob = user()
        val course = TestData.course()
        progress.markLessonCompleted(alice, course.id, TestData.lesson(course, 1).id)
        progress.markLessonCompleted(bob, course.id, TestData.lesson(course, 2).id)

        val rows = progress.findByUser(alice)

        assertEquals(1, rows.size)
        assertTrue(rows.all { it.userId == alice })
    }

    @Test
    fun `countCompletedLessons should count lessons of given course only`() {
        val userId = user()
        val kotlin = TestData.course("Kotlin")
        val ktor = TestData.course("Ktor")
        progress.markLessonCompleted(userId, kotlin.id, TestData.lesson(kotlin, 1).id)
        progress.markLessonCompleted(userId, kotlin.id, TestData.lesson(kotlin, 2).id)
        progress.markLessonCompleted(userId, ktor.id, TestData.lesson(ktor, 1).id)

        assertEquals(2, progress.countCompletedLessons(userId, kotlin.id))
        assertEquals(1, progress.countCompletedLessons(userId, ktor.id))
    }

    @Test
    fun `markLessonCompleted twice should not duplicate row`() {
        val userId = user()
        val course = TestData.course()
        val lesson = TestData.lesson(course, 1)

        val first = progress.markLessonCompleted(userId, course.id, lesson.id)
        val second = progress.markLessonCompleted(userId, course.id, lesson.id)

        assertTrue(first)
        assertFalse(second)
        assertEquals(1, progress.findByUser(userId).size)
    }

    @Test
    fun `courseCompletion should count completed lessons against all lessons of course`() {
        val userId = user()
        val course = TestData.course()
        val lessons = (1..4).map { TestData.lesson(course, it) }
        progress.markLessonCompleted(userId, course.id, lessons[0].id)
        progress.markLessonCompleted(userId, course.id, lessons[2].id)
        // чужая отметка не считается
        progress.markLessonCompleted(user(), course.id, lessons[1].id)

        val completion = progress.courseCompletion(userId, course.id)

        assertEquals(2, completion.completed)
        assertEquals(4, completion.total)
        assertEquals(50, completion.percentage)
    }

    @Test
    fun `courseCompletion should return zeros for course without lessons`() {
        val completion = progress.courseCompletion(user(), TestData.course().id)

        assertEquals(0, completion.total)
        assertEquals(0, completion.percentage)
    }

    @Test
    fun `upsertCourseProgress should create course row`() {
        val userId = user()
        val course = TestData.course()

        progress.upsertCourseProgress(userId, course.id, completed = false)

        val row = progress.findByUser(userId).single()
        assertEquals(ProgressType.COURSE, row.type)
        assertFalse(row.isCompleted)
        assertNull(row.completedAt)
    }

    @Test
    fun `upsertCourseProgress should update existing row instead of duplicating`() {
        val userId = user()
        val course = TestData.course()

        progress.upsertCourseProgress(userId, course.id, completed = false)
        progress.upsertCourseProgress(userId, course.id, completed = true)

        val rows = progress.findByUser(userId)
        assertEquals(1, rows.size)
        assertTrue(rows.single().isCompleted)
        assertNotNull(rows.single().completedAt)
    }

    @Test
    fun `progress rows should be removed together with lesson`() {
        val userId = user()
        val course = TestData.course()
        val lesson = TestData.lesson(course, 1)
        progress.markLessonCompleted(userId, course.id, lesson.id)

        LessonRepository().deleteLesson(lesson.id)

        assertTrue(progress.findByUser(userId).isEmpty())
    }
}
