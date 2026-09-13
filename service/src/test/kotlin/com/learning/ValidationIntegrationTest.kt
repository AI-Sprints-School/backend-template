package com.learning

import org.junit.jupiter.api.Tag
import com.learning.integration.DatabaseTestBase
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Интеграционные тесты валидации для API эндпоинтов
 * Проверяют, что валидация работает корректно на уровне HTTP запросов
 */
@Tag("core")
@Tag("chapter5")
class ValidationIntegrationTest : DatabaseTestBase() {

    private val baseUrl = ""

    // ==================== Регистрация - Валидация Email ====================

    @Test
    fun `registration should fail with invalid email format`() = apiTest {
        val invalidEmailData = """
            {
                "email": "not-an-email",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(invalidEmailData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"), "Expected VALIDATION_ERROR in response")
        assertTrue(body.contains("email") || body.contains("Email"), "Expected email validation error")
    }

    @Test
    fun `registration should fail with empty email`() = apiTest {
        val emptyEmailData = """
            {
                "email": "",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(emptyEmailData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR") || body.contains("error"))
    }

    @Test
    fun `registration should fail with too short email`() = apiTest {
        val shortEmailData = """
            {
                "email": "a@",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(shortEmailData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("error"))
    }

    // ==================== Регистрация - Валидация Password ====================

    @Test
    fun `registration should fail with weak password - too short`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val weakPasswordData = """
            {
                "email": "test$timestamp@example.com",
                "password": "weak",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(weakPasswordData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("password") || body.contains("Пароль"))
    }

    @Test
    fun `registration should fail with password without uppercase letter`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val noUppercaseData = """
            {
                "email": "test$timestamp@example.com",
                "password": "password123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(noUppercaseData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("password") || body.contains("заглавную"))
    }

    @Test
    fun `registration should fail with password without lowercase letter`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val noLowercaseData = """
            {
                "email": "test$timestamp@example.com",
                "password": "PASSWORD123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(noLowercaseData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("password") || body.contains("строчную"))
    }

    @Test
    fun `registration should fail with password without digit`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val noDigitData = """
            {
                "email": "test$timestamp@example.com",
                "password": "PasswordOnly",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(noDigitData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("password") || body.contains("цифр"))
    }

    // ==================== Регистрация - Валидация Имени ====================

    @Test
    fun `registration should fail with too short first name`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val shortFirstNameData = """
            {
                "email": "test$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "A",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(shortFirstNameData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("firstName") || body.contains("Имя"))
    }

    @Test
    fun `registration should fail with too short last name`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val shortLastNameData = """
            {
                "email": "test$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "B"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(shortLastNameData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        assertTrue(body.contains("lastName") || body.contains("Фамилия"))
    }

    @Test
    fun `registration should fail with invalid characters in first name`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val invalidFirstNameData = """
            {
                "email": "test$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "Иван123",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(invalidFirstNameData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
    }

    // ==================== Регистрация - Множественные ошибки ====================

    @Test
    fun `registration should fail with multiple validation errors`() = apiTest {
        val multipleErrorsData = """
            {
                "email": "invalid",
                "password": "weak",
                "firstName": "A",
                "lastName": "B"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(multipleErrorsData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
        // Должны быть ошибки для всех полей
        val hasMultipleErrors = listOf("email", "password", "firstName", "lastName")
            .count { body.contains(it, ignoreCase = true) } >= 2
        assertTrue(hasMultipleErrors, "Expected multiple validation errors")
    }

    // ==================== Регистрация - Успешная валидация ====================

    @Test
    fun `registration should succeed with valid data`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val validData = """
            {
                "email": "validuser$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(validData)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("accessToken"))
        assertTrue(body.contains("refreshToken"))
    }

    // ==================== Вход - Валидация ====================

    @Test
    fun `login should fail with invalid email format`() = apiTest {
        val invalidLoginData = """
            {
                "email": "not-valid-email",
                "password": "SomePassword123"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(invalidLoginData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
    }

    @Test
    fun `login should fail with empty password`() = apiTest {
        val emptyPasswordData = """
            {
                "email": "user@example.com",
                "password": ""
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(emptyPasswordData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("error"))
    }

    // ==================== Обновление профиля - Валидация ====================

    @Test
    fun `update profile should fail with invalid email format`() = apiTest {
        // Сначала регистрируемся
        val timestamp = System.currentTimeMillis()
        val registerData = """
            {
                "email": "profiletest$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "Тест",
                "lastName": "Пользователь"
            }
        """.trimIndent()

        val registerResponse = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerData)
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val accessToken = extractAccessToken(registerResponse.bodyAsText())

        // Пытаемся обновить профиль с невалидным email
        val invalidUpdateData = """
            {
                "email": "not-valid-email",
                "firstName": "Новое",
                "lastName": "Имя",
                "avatar": null
            }
        """.trimIndent()

        val updateResponse = client.put("$baseUrl/api/v1/user/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(invalidUpdateData)
        }

        assertEquals(HttpStatusCode.BadRequest, updateResponse.status)
        val body = updateResponse.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
    }

    @Test
    fun `update profile should fail with too short first name`() = apiTest {
        // Сначала регистрируемся
        val timestamp = System.currentTimeMillis()
        val registerData = """
            {
                "email": "profiletest2$timestamp@example.com",
                "password": "SecurePass123",
                "firstName": "Тест",
                "lastName": "Пользователь"
            }
        """.trimIndent()

        val registerResponse = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerData)
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val accessToken = extractAccessToken(registerResponse.bodyAsText())

        // Пытаемся обновить профиль с коротким именем
        val invalidUpdateData = """
            {
                "firstName": "A",
                "lastName": null,
                "email": null,
                "avatar": null
            }
        """.trimIndent()

        val updateResponse = client.put("$baseUrl/api/v1/user/profile") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            setBody(invalidUpdateData)
        }

        assertEquals(HttpStatusCode.BadRequest, updateResponse.status)
        val body = updateResponse.bodyAsText()
        assertTrue(body.contains("VALIDATION_ERROR"))
    }

    // ==================== Тесты форматов ошибок ====================

    @Test
    fun `validation error should have correct structure`() = apiTest {
        val invalidData = """
            {
                "email": "invalid",
                "password": "weak",
                "firstName": "A",
                "lastName": "B"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(invalidData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()

        // Проверяем структуру ответа
        assertTrue(body.contains("error"), "Response should contain 'error' field")
        assertTrue(body.contains("message"), "Response should contain 'message' field")
        assertTrue(body.contains("VALIDATION_ERROR"), "Error type should be VALIDATION_ERROR")
    }

    @Test
    fun `validation error should contain detailed message`() = apiTest {
        val invalidData = """
            {
                "email": "invalid-email",
                "password": "SecurePass123",
                "firstName": "Иван",
                "lastName": "Иванов"
            }
        """.trimIndent()

        val response = client.post("$baseUrl/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(invalidData)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()

        // Проверяем, что есть детали ошибки
        assertTrue(
            body.contains("details") || body.contains("email"),
            "Response should contain details about validation error"
        )
    }

    // ==================== Вспомогательные функции ====================

    private fun extractAccessToken(jsonResponse: String): String {
        val cleanResponse = jsonResponse.replace("\n", "").replace("\r", "").replace(" ", "")
        val tokenStart = cleanResponse.indexOf("\"accessToken\":\"") + 15
        if (tokenStart == 14) {
            throw IllegalArgumentException("Access token not found in response")
        }
        val tokenEnd = cleanResponse.indexOf("\"", tokenStart)
        if (tokenEnd == -1) {
            throw IllegalArgumentException("Invalid token format in response")
        }
        return cleanResponse.substring(tokenStart, tokenEnd)
    }

}

