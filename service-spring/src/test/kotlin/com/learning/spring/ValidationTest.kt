package com.learning.spring

import com.learning.spring.support.IntegrationTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Урок 7: 400 в формате `ErrorResponse` Ktor-версии (плоские `error`, `message`, `details`)
 * на регистрации и создании курса. Запрос курса идёт с токеном администратора: до главы 3
 * его игнорируют, с главы 3 он нужен.
 */
@Tag("chapter2")
class ValidationTest : IntegrationTest() {

    private fun register(email: String = "ok@example.com", password: String = "SecurePass123", first: String = "Иван", last: String = "Иванов") =
        http.post("/api/v1/auth/register", """{"email":"$email","password":"$password","firstName":"$first","lastName":"$last"}""")

    private fun assertValidation(response: com.learning.spring.support.Resp, field: String) {
        assertEquals(400, response.status, response.body)
        assertEquals("VALIDATION_ERROR", response.json["error"].asString())
        assertTrue(response.json["details"].asString().contains(field, ignoreCase = true), "В details нет поля $field: ${response.body}")
    }

    @Test
    @Tag("core")
    fun `registration with invalid email returns 400 with details`() = assertValidation(register(email = "not-an-email"), "email")

    @Test
    @Tag("core")
    fun `registration with short password returns 400 with details`() = assertValidation(register(password = "Ab1"), "password")

    @Test
    @Tag("core")
    fun `registration reports all invalid fields at once`() {
        val response = register(email = "bad", password = "x", first = "И")
        assertValidation(response, "email")
        assertTrue(response.json["details"].asString().contains("password", ignoreCase = true), response.body)
    }

    @Test
    @Tag("core")
    fun `course with blank title returns 400`() {
        val admin = users.admin()
        val response = http.post(
            "/api/v1/courses",
            """{"title":"","description":"Описание курса для проверки","category":"kotlin","difficulty":"BEGINNER","duration":10,"price":100.0}""",
            admin.token,
        )
        assertValidation(response, "title")
    }

    @Test
    @Tag("core")
    fun `course with negative price returns 400`() {
        val admin = users.admin()
        val response = http.post(
            "/api/v1/courses",
            """{"title":"Курс про Spring","description":"Описание курса для проверки","category":"kotlin","difficulty":"BEGINNER","duration":10,"price":-1.0}""",
            admin.token,
        )
        assertValidation(response, "price")
    }

    @Test
    @Tag("extension")
    fun `registration with password without uppercase letter returns 400`() = assertValidation(register(password = "securepass123"), "password")

    @Test
    @Tag("extension")
    fun `registration with short first name returns 400`() = assertValidation(register(first = "И"), "firstName")

    @Test
    @Tag("extension")
    fun `registration with digits in last name returns 400`() = assertValidation(register(last = "Иванов123"), "lastName")

    @Test
    @Tag("extension")
    fun `login with empty password returns 400`() {
        val response = http.post("/api/v1/auth/login", """{"email":"ok@example.com","password":""}""")
        assertValidation(response, "password")
    }
}
