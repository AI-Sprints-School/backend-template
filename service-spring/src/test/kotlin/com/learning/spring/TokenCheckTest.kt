package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

/** Урок 13: `/auth/me` и плохие токены — просроченный, с чужой подписью, мусор. */
@Tag("core")
@Tag("chapter3")
class TokenCheckTest : IntegrationTest() {

    @Test
    fun `valid token opens auth me`() {
        val student = users.student()
        val response = http.get("/api/v1/auth/me", student.token)
        assertEquals(200, response.status, response.body)
        assertEquals(student.email, response.json["email"].asString())
    }

    @Test
    fun `expired token returns 401`() {
        val student = users.student()
        val expired = users.accessToken(student.id, student.email, "student", ttlSeconds = -60)
        assertEquals(401, http.get("/api/v1/auth/me", expired).status)
    }

    @Test
    fun `token signed with another secret returns 401`() {
        val student = users.student()
        val forged = users.accessToken(student.id, student.email, "admin", secret = "another-secret-another-secret-123456")
        assertEquals(401, http.get("/api/v1/auth/me", forged).status)
    }

    @Test
    fun `garbage token returns 401`() = assertEquals(401, http.get("/api/v1/auth/me", "not.a.token").status)

    @Test
    fun `token of unknown user returns 401 or 404 not 200`() {
        val ghost = users.accessToken(UUID.randomUUID(), "ghost@example.com", "student")
        val status = http.get("/api/v1/auth/me", ghost).status
        assertEquals(true, status == 401 || status == 404, "Неизвестный пользователь получил $status")
    }
}
