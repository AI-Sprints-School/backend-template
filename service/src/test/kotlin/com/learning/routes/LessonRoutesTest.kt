package com.learning.routes

import com.learning.*
import com.learning.integration.DatabaseTestBase
import com.learning.repositories.LessonRepository
import io.ktor.client.request.*
import io.ktor.http.*
import org.junit.jupiter.api.Tag
import java.util.*
import kotlin.test.*

/**
 * HTTP-тесты CRUD уроков. Чтение открыто всем, запись — только роли admin.
 */
@Tag("core")
class LessonRoutesTest : DatabaseTestBase() {

    private val lessons = LessonRepository()

    private fun body(courseId: String, title: String = "Маршруты в Ktor") = """
        {
            "courseId": "$courseId",
            "title": "$title",
            "description": "Как устроен роутинг в Ktor",
            "orderIndex": 1,
            "duration": 20
        }
    """.trimIndent()

    // ==================== Чтение ====================

    @Test
    @Tag("chapter4")
    fun `GET lessons should list lessons`() = apiTest {
        val course = TestData.course()
        TestData.lesson(course, 1)
        TestData.lesson(course, 2)

        val response = client.get("/api/v1/lessons")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, response.json().arr("lessons").size)
    }

    @Test
    @Tag("chapter4")
    fun `GET lesson by id should return lesson`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1, title = "Первый урок")

        val response = client.get("/api/v1/lessons/${lesson.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.json()
        assertEquals(lesson.id.toString(), body.str("id"))
        assertEquals("Первый урок", body.str("title"))
    }

    @Test
    @Tag("chapter4")
    fun `GET lesson by id should return 404 for unknown lesson`() = apiTest {
        val response = client.get("/api/v1/lessons/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    @Tag("chapter4")
    fun `GET lesson content should return text block`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1, content = "Роутинг — это дерево")

        val response = client.get("/api/v1/lessons/${lesson.id}/content")

        assertEquals(HttpStatusCode.OK, response.status)
        val contents = response.json().arr("contents")
        assertEquals(1, contents.size)
        assertTrue(contents.single().toString().contains("Роутинг — это дерево"))
    }

    // ==================== Создание ====================

    @Test
    @Tag("chapter5")
    fun `POST lesson without token should return 401`() = apiTest {
        val course = TestData.course()

        val response = client.post("/api/v1/lessons") {
            contentType(ContentType.Application.Json)
            setBody(body(course.id.toString()))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(lessons.findByCourseId(course.id).isEmpty())
    }

    @Test
    @Tag("chapter5")
    fun `POST lesson as student should return 403`() = apiTest {
        val course = TestData.course()
        val student = TestUsers.student()

        val response = client.post("/api/v1/lessons") {
            header(HttpHeaders.Authorization, student.bearer)
            contentType(ContentType.Application.Json)
            setBody(body(course.id.toString()))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(lessons.findByCourseId(course.id).isEmpty())
    }

    @Test
    @Tag("chapter5")
    fun `POST lesson as admin should return 201 and persist lesson`() = apiTest {
        val course = TestData.course()
        val admin = TestUsers.admin()

        val response = client.post("/api/v1/lessons") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(body(course.id.toString()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val stored = lessons.findByCourseId(course.id)
        assertEquals(listOf("Маршруты в Ktor"), stored.map { it.title })
        assertEquals(stored.single().id.toString(), response.json().str("id"))
    }

    @Test
    @Tag("chapter5")
    fun `POST lesson as admin with malformed course id should return 400`() = apiTest {
        val admin = TestUsers.admin()

        val response = client.post("/api/v1/lessons") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(body("not-a-uuid"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", response.json().str("error"))
    }

    // ==================== Обновление ====================

    @Test
    @Tag("chapter5")
    fun `PUT lesson as student should return 403`() = apiTest {
        val course = TestData.course()
        val lesson = TestData.lesson(course, 1, title = "Старое название")
        val student = TestUsers.student()

        val response = client.put("/api/v1/lessons/${lesson.id}") {
            header(HttpHeaders.Authorization, student.bearer)
            contentType(ContentType.Application.Json)
            setBody(body(course.id.toString(), title = "Новое название"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Старое название", lessons.findById(lesson.id)?.title)
    }

    @Test
    @Tag("chapter5")
    fun `PUT lesson as admin should update lesson`() = apiTest {
        val course = TestData.course()
        val lesson = TestData.lesson(course, 1, title = "Старое название")
        val admin = TestUsers.admin()

        val response = client.put("/api/v1/lessons/${lesson.id}") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(body(course.id.toString(), title = "Новое название"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Новое название", lessons.findById(lesson.id)?.title)
    }

    // ==================== Удаление ====================

    @Test
    @Tag("chapter5")
    fun `DELETE lesson without token should return 401`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1)

        val response = client.delete("/api/v1/lessons/${lesson.id}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNotNull(lessons.findById(lesson.id))
    }

    @Test
    @Tag("chapter5")
    fun `DELETE lesson as student should return 403`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1)
        val student = TestUsers.student()

        val response = client.delete("/api/v1/lessons/${lesson.id}") {
            header(HttpHeaders.Authorization, student.bearer)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertNotNull(lessons.findById(lesson.id))
    }

    @Test
    @Tag("chapter5")
    fun `DELETE lesson as admin should return 204 and remove lesson`() = apiTest {
        val lesson = TestData.lesson(TestData.course(), 1)
        val admin = TestUsers.admin()

        val response = client.delete("/api/v1/lessons/${lesson.id}") {
            header(HttpHeaders.Authorization, admin.bearer)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(lessons.findById(lesson.id))
    }
}
