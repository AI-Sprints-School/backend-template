package com.learning.integration

import com.learning.TestData
import com.learning.repositories.LessonRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Интеграционные тесты LessonRepository с реальной PostgreSQL.
 */
@Tag("core")
@Tag("chapter3")
class LessonRepositoryIntegrationTest : DatabaseTestBase() {

    private val lessons = LessonRepository()

    @Test
    fun `createLesson should create lesson and return it`() {
        val course = TestData.course()

        val lesson = lessons.createLesson(
            courseId = course.id,
            title = "Роутинг",
            description = "Дерево маршрутов",
            content = "Текст",
            videoUrl = "https://example.com/v.mp4",
            duration = 25,
            order = 1,
            isPublished = true
        )

        assertNotNull(lesson)
        assertEquals(course.id, lesson!!.courseId)
        assertEquals("Роутинг", lesson.title)
        assertEquals(25, lesson.duration)
        assertTrue(lesson.isPublished)
    }

    @Test
    fun `createLesson and deleteLesson should keep course lessons count in sync`() {
        val courses = com.learning.repositories.CourseRepository()
        val course = TestData.course()
        val first = TestData.lesson(course, 1)
        TestData.lesson(course, 2)

        assertEquals(2, courses.findById(course.id)?.lessonsCount)

        lessons.deleteLesson(first.id)

        assertEquals(1, courses.findById(course.id)?.lessonsCount)
    }

    @Test
    fun `createLesson should return null when course not exists`() {
        val lesson = lessons.createLesson(UUID.randomUUID(), "Урок", null, "Текст", null, 10, 1)

        assertNull(lesson)
    }

    @Test
    fun `findById should return null when not exists`() {
        assertNull(lessons.findById(UUID.randomUUID()))
    }

    @Test
    fun `findByCourseId should return lessons ordered by order`() {
        val course = TestData.course()
        TestData.lesson(course, 3, title = "Третий")
        TestData.lesson(course, 1, title = "Первый")
        TestData.lesson(course, 2, title = "Второй")

        val result = lessons.findByCourseId(course.id)

        assertEquals(listOf("Первый", "Второй", "Третий"), result.map { it.title })
    }

    @Test
    fun `findByCourseId should not return lessons of other courses`() {
        val kotlin = TestData.course("Kotlin")
        val ktor = TestData.course("Ktor")
        TestData.lesson(kotlin, 1)
        TestData.lesson(ktor, 1)

        val result = lessons.findByCourseId(kotlin.id)

        assertEquals(1, result.size)
        assertEquals(kotlin.id, result.single().courseId)
    }

    @Test
    fun `findPublishedByCourseId should return only published lessons`() {
        val course = TestData.course()
        TestData.lesson(course, 1, title = "Опубликован")
        lessons.createLesson(course.id, "Черновик", null, "Текст", null, 10, 2, isPublished = false)

        val result = lessons.findPublishedByCourseId(course.id)

        assertEquals(listOf("Опубликован"), result.map { it.title })
    }

    @Test
    fun `findAll should return lessons of all courses`() {
        val kotlin = TestData.course("Kotlin")
        val ktor = TestData.course("Ktor")
        TestData.lesson(kotlin, 1)
        TestData.lesson(kotlin, 2)
        TestData.lesson(ktor, 1)

        assertEquals(3, lessons.findAll().size)
    }

    @Test
    fun `updateLesson should update only provided fields`() {
        val lesson = TestData.lesson(TestData.course(), 1, title = "Старое", duration = 15)

        val success = lessons.updateLesson(lesson.id, title = "Новое")

        assertTrue(success)
        val updated = lessons.findById(lesson.id)!!
        assertEquals("Новое", updated.title)
        assertEquals(15, updated.duration)
    }

    @Test
    fun `updateLesson should return false when lesson not exists`() {
        assertFalse(lessons.updateLesson(UUID.randomUUID(), title = "Нет такого"))
    }

    @Test
    fun `deleteLesson should delete lesson and return true`() {
        val lesson = TestData.lesson(TestData.course(), 1)

        assertTrue(lessons.deleteLesson(lesson.id))
        assertNull(lessons.findById(lesson.id))
    }

    @Test
    fun `deleteLesson should return false when lesson not exists`() {
        assertFalse(lessons.deleteLesson(UUID.randomUUID()))
    }

    @Test
    fun `getNextLesson and getPreviousLesson should follow order`() {
        val course = TestData.course()
        TestData.lesson(course, 1, title = "Первый")
        TestData.lesson(course, 2, title = "Второй")
        TestData.lesson(course, 3, title = "Третий")

        assertEquals("Третий", lessons.getNextLesson(course.id, 2)?.title)
        assertEquals("Первый", lessons.getPreviousLesson(course.id, 2)?.title)
        assertNull(lessons.getNextLesson(course.id, 3))
    }

    @Test
    fun `getTotalDuration should sum durations of course lessons`() {
        val course = TestData.course()
        TestData.lesson(course, 1, duration = 10)
        TestData.lesson(course, 2, duration = 35)

        assertEquals(45, lessons.getTotalDuration(course.id))
    }
}
