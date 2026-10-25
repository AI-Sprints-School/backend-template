package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Урок 10: прогресс ученика — `POST /api/v1/lessons/{id}/complete` и `GET /api/v1/user/progress`.
 * Пользователя сервис узнаёт по `sub` токена (как у Ktor-версии).
 */
@Tag("core")
@Tag("chapter2")
class ProgressTest : IntegrationTest() {

    @Test
    fun `complete returns course completion percentage`() {
        val course = data.course()
        val first = data.lesson(course, 1)
        (2..4).forEach { data.lesson(course, it) }
        val student = users.student()

        val response = http.post("/api/v1/lessons/${first.id}/complete", bearer = student.token)

        assertEquals(200, response.status, response.body)
        val progress = response.json["progress"]
        assertEquals(1, progress["completedLessons"].asInt())
        assertEquals(4, progress["totalLessons"].asInt())
        assertEquals(25, progress["courseCompletionPercentage"].asInt())
        assertFalse(progress["courseCompleted"].asBoolean())
    }

    @Test
    fun `completing all lessons marks the course completed`() {
        val course = data.course()
        val lessons = listOf(data.lesson(course, 1, duration = 10), data.lesson(course, 2, duration = 25))
        val student = users.student()

        lessons.forEach { assertEquals(200, http.post("/api/v1/lessons/${it.id}/complete", bearer = student.token).status) }
        val progress = http.get("/api/v1/user/progress", student.token).json

        assertEquals(1, progress["completedCourses"].asInt())
        assertEquals(2, progress["completedLessons"].asInt())
        assertEquals(35, progress["totalStudyTime"].asInt())
    }

    @Test
    fun `completing the same lesson twice counts once`() {
        val course = data.course()
        val lesson = data.lesson(course, 1)
        data.lesson(course, 2)
        val student = users.student()

        repeat(2) { http.post("/api/v1/lessons/${lesson.id}/complete", bearer = student.token) }

        assertEquals(1, http.get("/api/v1/user/progress", student.token).json["completedLessons"].asInt())
    }

    @Test
    fun `complete of unknown lesson returns 404`() {
        val response = http.post("/api/v1/lessons/${UUID.randomUUID()}/complete", bearer = users.student().token)
        assertEquals(404, response.status)
        assertTrue(response.json.has("error"))
    }
}
