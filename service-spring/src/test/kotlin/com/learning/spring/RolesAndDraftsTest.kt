package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Урок 14: 401 против 403, запись курсов — `admin`; черновик по id — автору и `admin`, остальным 404. */
@Tag("chapter3")
class RolesAndDraftsTest : IntegrationTest() {

    private val courseBody =
        """{"title":"Курс про Spring","description":"Описание курса для проверки","category":"kotlin","difficulty":"BEGINNER","duration":10,"price":100.0}"""

    @Test
    @Tag("core")
    fun `student cannot create a course`() = assertEquals(403, http.post("/api/v1/courses", courseBody, users.student().token).status)

    @Test
    @Tag("core")
    fun `admin creates a course`() = assertEquals(201, http.post("/api/v1/courses", courseBody, users.admin().token).let { it.status })

    @Test
    @Tag("core")
    fun `draft is hidden from anonymous with 404`() {
        val draft = data.course(published = false, authorId = users.student().id)
        assertEquals(404, http.get("/api/v1/courses/${draft.id}").status)
    }

    @Test
    @Tag("core")
    fun `draft is hidden from another student with 404`() {
        val draft = data.course(published = false, authorId = users.student().id)
        assertEquals(404, http.get("/api/v1/courses/${draft.id}", users.student().token).status)
    }

    @Test
    @Tag("core")
    fun `draft is visible to its author`() {
        val author = users.student()
        val draft = data.course(published = false, authorId = author.id)
        assertEquals(200, http.get("/api/v1/courses/${draft.id}", author.token).status)
    }

    @Test
    @Tag("core")
    fun `draft is visible to admin`() {
        val draft = data.course(published = false, authorId = users.student().id)
        assertEquals(200, http.get("/api/v1/courses/${draft.id}", users.admin().token).status)
    }

    @Test
    @Tag("extension")
    fun `draft lessons are hidden from anonymous with 404`() {
        val draft = data.course(published = false, authorId = users.student().id)
        data.lesson(draft, 1)
        assertEquals(404, http.get("/api/v1/courses/${draft.id}/lessons").status)
    }

    @Test
    @Tag("extension")
    fun `draft lessons are visible to author`() {
        val author = users.student()
        val draft = data.course(published = false, authorId = author.id)
        data.lesson(draft, 1)
        assertEquals(200, http.get("/api/v1/courses/${draft.id}/lessons", author.token).status)
    }
}
