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
 * Интеграционные тесты для API: приложение целиком через Ktor Test, база — Testcontainers
 */
class ApiIntegrationTest : DatabaseTestBase() {

    @Test
    @Tag("core")
    @Tag("chapter1")
    fun `test health endpoint`() = apiTest {
        val response = client.get("$BASE_URL/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("UP"))
    }

    @Test
    @Tag("extension")
    @Tag("chapter4")
    fun `test public endpoints`() = apiTest {
        // Test courses endpoint
        val coursesResponse = client.get("$BASE_URL/api/v1/courses")
        assertEquals(HttpStatusCode.OK, coursesResponse.status)
        assertTrue(coursesResponse.bodyAsText().contains("courses"))

        // Test tests endpoint
        val testsResponse = client.get("$BASE_URL/api/v1/tests")
        assertEquals(HttpStatusCode.OK, testsResponse.status)
        assertTrue(testsResponse.bodyAsText().contains("tests"))

        // Test sprints endpoint
        val sprintsResponse = client.get("$BASE_URL/api/v1/sprints")
        assertEquals(HttpStatusCode.OK, sprintsResponse.status)
        assertTrue(sprintsResponse.bodyAsText().contains("sprints"))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test user registration`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val testEmail = "testuser$timestamp@example.com"
        val testPassword = "Password123"
        val testFirstName = "Test"
        val testLastName = "User"

        val registrationData = """
            {
                "email": "$testEmail",
                "password": "$testPassword",
                "firstName": "$testFirstName",
                "lastName": "$testLastName"
            }
        """.trimIndent()

        val response = client.post("$BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registrationData)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("accessToken"))
        assertTrue(responseBody.contains("refreshToken"))
        assertTrue(responseBody.contains(testEmail))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test user login`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val testEmail = "testuser$timestamp@example.com"
        val testPassword = "Password123"

        // First register a user
        val registrationData = """
            {
                "email": "$testEmail",
                "password": "$testPassword",
                "firstName": "Test",
                "lastName": "User"
            }
        """.trimIndent()

        val registrationResponse = client.post("$BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registrationData)
        }
        assertEquals(HttpStatusCode.Created, registrationResponse.status)

        // Then login
        val loginData = """
            {
                "email": "$testEmail",
                "password": "$testPassword"
            }
        """.trimIndent()

        val loginResponse = client.post("$BASE_URL/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(loginData)
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginBody = loginResponse.bodyAsText()
        assertTrue(loginBody.contains("accessToken"))
        assertTrue(loginBody.contains("refreshToken"))
        assertTrue(loginBody.contains(testEmail))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test protected endpoint`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val testEmail = "testuser$timestamp@example.com"
        val testPassword = "Password123"

        // Register and login
        val registrationData = """
            {
                "email": "$testEmail",
                "password": "$testPassword",
                "firstName": "Test",
                "lastName": "User"
            }
        """.trimIndent()

        val registrationResponse = client.post("$BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registrationData)
        }
        assertEquals(HttpStatusCode.Created, registrationResponse.status)

        // Extract access token
        val responseBody = registrationResponse.bodyAsText()
        val accessToken = extractAccessToken(responseBody)

        // Test protected endpoint
        val profileResponse = client.get("$BASE_URL/api/v1/user/profile") {
            header("Authorization", "Bearer $accessToken")
        }
        assertEquals(HttpStatusCode.OK, profileResponse.status)
        val profileBody = profileResponse.bodyAsText()
        assertTrue(profileBody.contains("email"))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test invalid credentials`() = apiTest {
        val loginData = """
            {
                "email": "wrong@example.com",
                "password": "wrongpassword"
            }
        """.trimIndent()

        val response = client.post("$BASE_URL/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(loginData)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("error"))
        assertTrue(responseBody.contains("Неверный email или пароль"))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test duplicate registration`() = apiTest {
        val timestamp = System.currentTimeMillis()
        val testEmail = "duplicate$timestamp@example.com"
        val testPassword = "Password123"

        val registrationData = """
            {
                "email": "$testEmail",
                "password": "$testPassword",
                "firstName": "Test",
                "lastName": "User"
            }
        """.trimIndent()

        // First registration should succeed
        val firstResponse = client.post("$BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registrationData)
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Second registration should fail
        val secondResponse = client.post("$BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registrationData)
        }
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        val responseBody = secondResponse.bodyAsText()
        assertTrue(responseBody.contains("error"))
        assertTrue(responseBody.contains("Пользователь с таким email уже существует"))
    }

    @Test
    @Tag("core")
    @Tag("chapter5")
    fun `test unauthorized access`() = apiTest {
        val response = client.get("$BASE_URL/api/v1/user/profile")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("error"))
    }

    private fun extractAccessToken(jsonResponse: String): String {
        // Simple JSON parsing to extract access token
        val cleanResponse = jsonResponse.replace("\n", "").replace("\r", "").replace(" ", "")
        val tokenStart = cleanResponse.indexOf("\"accessToken\":\"") + 15
        if (tokenStart == 14) { // -1 + 15 = 14 if not found
            throw IllegalArgumentException("Access token not found in response: $jsonResponse")
        }
        val tokenEnd = cleanResponse.indexOf("\"", tokenStart)
        if (tokenEnd == -1) {
            throw IllegalArgumentException("Invalid token format in response: $jsonResponse")
        }
        return cleanResponse.substring(tokenStart, tokenEnd)
    }

    companion object {
        const val BASE_URL = ""
    }

}
