package com.learning.routes

import com.learning.*
import com.learning.integration.DatabaseTestBase
import com.learning.repositories.CourseRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Tag
import java.util.*
import kotlin.test.*

/**
 * HTTP-тесты CRUD курсов: приложение целиком через Ktor Test, база — Testcontainers.
 * Чтение открыто всем, запись — только роли admin (без токена 401, без роли 403).
 */
@Tag("core")
class CourseRoutesTest : DatabaseTestBase() {

    private val courses = CourseRepository()

    private val validBody = """
        {
            "title": "Ktor с нуля",
            "description": "REST-сервис на Ktor по спецификации",
            "category": "Backend",
            "difficulty": "beginner",
            "duration": 600,
            "price": 1990.0
        }
    """.trimIndent()

    // ==================== Чтение ====================

    @Test
    @Tag("chapter4")
    fun `GET courses should list only published courses`() = apiTest {
        TestData.course("Опубликованный", published = true)
        TestData.course("Черновик", published = false)

        val response = client.get("/api/v1/courses")

        assertEquals(HttpStatusCode.OK, response.status)
        val titles = response.json().arr("courses").map { it.toString() }
        assertEquals(1, titles.size)
        assertTrue(titles.single().contains("Опубликованный"))
    }

    @Test
    @Tag("chapter4")
    fun `GET course by id should return course`() = apiTest {
        val course = TestData.course("Корутины")

        val response = client.get("/api/v1/courses/${course.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.json()
        assertEquals(course.id.toString(), body.str("id"))
        assertEquals("Корутины", body.str("title"))
    }

    @Test
    @Tag("chapter4")
    fun `GET course by id should return 404 for unknown course`() = apiTest {
        val response = client.get("/api/v1/courses/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("NOT_FOUND", response.json().str("error"))
    }

    @Test
    @Tag("chapter4")
    fun `GET course by id should return 400 for invalid id`() = apiTest {
        val response = client.get("/api/v1/courses/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    @Tag("chapter4")
    fun `GET course lessons should return lessons in order`() = apiTest {
        val course = TestData.course()
        TestData.lesson(course, order = 2, title = "Второй")
        TestData.lesson(course, order = 1, title = "Первый")

        val response = client.get("/api/v1/courses/${course.id}/lessons")

        assertEquals(HttpStatusCode.OK, response.status)
        val lessons = response.json().arr("lessons").map { it.toString() }
        assertEquals(2, lessons.size)
        assertTrue(lessons[0].contains("Первый"))
        assertTrue(lessons[1].contains("Второй"))
    }

    // ==================== Создание ====================

    @Test
    @Tag("chapter5")
    fun `POST course without token should return 401`() = apiTest {
        val response = client.post("/api/v1/courses") {
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(courses.findAll().isEmpty())
    }

    @Test
    @Tag("chapter5")
    fun `POST course as student should return 403`() = apiTest {
        val student = TestUsers.student()

        val response = client.post("/api/v1/courses") {
            header(HttpHeaders.Authorization, student.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("AUTHORIZATION_ERROR", response.json().str("error"))
        assertTrue(courses.findAll().isEmpty())
    }

    @Test
    @Tag("chapter5")
    fun `POST course as admin should return 201 and persist course`() = apiTest {
        val admin = TestUsers.admin()

        val response = client.post("/api/v1/courses") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val id = UUID.fromString(response.json().str("id"))
        val stored = courses.findById(id)
        assertNotNull(stored)
        assertEquals("Ktor с нуля", stored.title)
        assertFalse(stored.isPublished, "Новый курс по умолчанию — черновик")
    }

    @Test
    @Tag("chapter5")
    fun `POST course as admin with invalid body should return 400 validation error`() = apiTest {
        val admin = TestUsers.admin()

        val response = client.post("/api/v1/courses") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody.replace("\"price\": 1990.0", "\"price\": -1.0"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_ERROR", response.json().str("error"))
        assertTrue(courses.findAll().isEmpty())
    }

    // ==================== Обновление ====================

    @Test
    @Tag("chapter5")
    fun `PUT course as student should return 403`() = apiTest {
        val course = TestData.course("Старое название")
        val student = TestUsers.student()

        val response = client.put("/api/v1/courses/${course.id}") {
            header(HttpHeaders.Authorization, student.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Старое название", courses.findById(course.id)?.title)
    }

    @Test
    @Tag("chapter5")
    fun `PUT course as admin should update course`() = apiTest {
        val course = TestData.course("Старое название")
        val admin = TestUsers.admin()

        val response = client.put("/api/v1/courses/${course.id}") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ktor с нуля", response.json().str("title"))
        assertEquals("Ktor с нуля", courses.findById(course.id)?.title)
    }

    @Test
    @Tag("chapter5")
    fun `PUT course as admin should return 404 for unknown course`() = apiTest {
        val admin = TestUsers.admin()

        val response = client.put("/api/v1/courses/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, admin.bearer)
            contentType(ContentType.Application.Json)
            setBody(validBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ==================== Удаление ====================

    @Test
    @Tag("chapter5")
    fun `DELETE course without token should return 401`() = apiTest {
        val course = TestData.course()

        val response = client.delete("/api/v1/courses/${course.id}")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNotNull(courses.findById(course.id))
    }

    @Test
    @Tag("chapter5")
    fun `DELETE course as student should return 403`() = apiTest {
        val course = TestData.course()
        val student = TestUsers.student()

        val response = client.delete("/api/v1/courses/${course.id}") {
            header(HttpHeaders.Authorization, student.bearer)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertNotNull(courses.findById(course.id))
    }

    @Test
    @Tag("chapter5")
    fun `DELETE course as admin should return 204 and remove course`() = apiTest {
        val course = TestData.course()
        val admin = TestUsers.admin()

        val response = client.delete("/api/v1/courses/${course.id}") {
            header(HttpHeaders.Authorization, admin.bearer)
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(courses.findById(course.id))
    }

    @Test
    @Tag("chapter5")
    fun `DELETE course as admin should return 404 for unknown course`() = apiTest {
        val admin = TestUsers.admin()

        val response = client.delete("/api/v1/courses/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, admin.bearer)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
