package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Уроки 6 и 9: чтение каталога по спецификации — опубликованные курсы, курс по id, уроки по порядку. Пути — с префиксом `/api/v1`, как у Ktor-двойника. */
@Tag("core")
@Tag("chapter2")
class CatalogRoutesTest : IntegrationTest() {

    @Test
    fun `GET courses lists only published courses`() {
        val published = data.course(title = "Опубликованный ${UUID.randomUUID()}")
        val draft = data.course(title = "Черновик ${UUID.randomUUID()}", published = false)

        val response = http.get("/api/v1/courses?limit=100")

        assertEquals(200, response.status, response.body)
        val titles = response.strings("data", "title")
        assertTrue(published.title in titles, "Опубликованного курса нет в каталоге")
        assertFalse(draft.title in titles, "Черновик попал в каталог")
    }

    @Test
    fun `GET course by id returns course`() {
        val course = data.course()

        val response = http.get("/api/v1/courses/${course.id}")

        assertEquals(200, response.status, response.body)
        assertEquals(course.id.toString(), response.json["id"].asString())
        assertEquals(course.title, response.json["title"].asString())
    }

    @Test
    fun `GET unknown course returns 404 with error body`() {
        val response = http.get("/api/v1/courses/${UUID.randomUUID()}")

        assertEquals(404, response.status)
        assertEquals("NOT_FOUND", response.json["error"].asString())
    }

    @Test
    fun `GET course with malformed id returns 400`() {
        assertEquals(400, http.get("/api/v1/courses/not-a-uuid").status)
    }

    @Test
    fun `GET course lessons returns lessons in order`() {
        val course = data.course()
        data.lesson(course, order = 2, title = "Второй")
        data.lesson(course, order = 1, title = "Первый")

        val response = http.get("/api/v1/courses/${course.id}/lessons")

        assertEquals(200, response.status, response.body)
        val titles = response.strings("lessons", "title")
        assertEquals(listOf("Первый", "Второй"), titles)
    }
}
