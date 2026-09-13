package com.learning.services

import org.junit.jupiter.api.Tag
import com.learning.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.*

/**
 * Unit тесты для JwtService
 */
@Tag("core")
@Tag("chapter5")
class JwtServiceTest {

    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setUp() {
        jwtService = JwtService()
    }

    // ==================== Access Token Tests ====================

    @Test
    fun `generateAccessToken should create valid token`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(jwtService.validateToken(token))
    }

    @Test
    fun `generateAccessToken should contain correct userId`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)
        val extractedUserId = jwtService.getUserId(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `generateAccessToken should contain correct email`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)
        val extractedEmail = jwtService.getEmail(token)

        assertEquals(email, extractedEmail)
    }

    @Test
    fun `generateAccessToken should have access type`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)
        val tokenType = jwtService.getTokenType(token)

        assertEquals("access", tokenType)
    }

    @Test
    fun `access token should not be expired immediately`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)

        assertFalse(jwtService.isTokenExpired(token))
    }

    // ==================== Refresh Token Tests ====================

    @Test
    fun `generateRefreshToken should create valid token`() {
        val userId = UUID.randomUUID().toString()

        val token = jwtService.generateRefreshToken(userId)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(jwtService.validateToken(token))
    }

    @Test
    fun `generateRefreshToken should contain correct userId`() {
        val userId = UUID.randomUUID().toString()

        val token = jwtService.generateRefreshToken(userId)
        val extractedUserId = jwtService.getUserId(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `generateRefreshToken should have refresh type`() {
        val userId = UUID.randomUUID().toString()

        val token = jwtService.generateRefreshToken(userId)
        val tokenType = jwtService.getTokenType(token)

        assertEquals("refresh", tokenType)
    }

    @Test
    fun `refresh token should not contain email`() {
        val userId = UUID.randomUUID().toString()

        val token = jwtService.generateRefreshToken(userId)
        val email = jwtService.getEmail(token)

        assertNull(email)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `validateToken should return false for invalid token`() {
        val invalidToken = "invalid.token.here"

        assertFalse(jwtService.validateToken(invalidToken))
    }

    @Test
    fun `validateToken should return false for empty token`() {
        assertFalse(jwtService.validateToken(""))
    }

    @Test
    fun `validateToken should return false for malformed token`() {
        assertFalse(jwtService.validateToken("not-a-jwt"))
    }

    @Test
    fun `validateToken should return false for tampered token`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)
        val tamperedToken = token.dropLast(5) + "XXXXX"

        assertFalse(jwtService.validateToken(tamperedToken))
    }

    // ==================== Claims Tests ====================

    @Test
    fun `getClaims should return null for invalid token`() {
        val claims = jwtService.getClaims("invalid.token")

        assertNull(claims)
    }

    @Test
    fun `getClaims should return claims for valid token`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token = jwtService.generateAccessToken(userId, email)
        val claims = jwtService.getClaims(token)

        assertNotNull(claims)
        assertEquals(userId, claims?.subject)
    }

    @Test
    fun `getUserId should return null for invalid token`() {
        val userId = jwtService.getUserId("invalid.token")

        assertNull(userId)
    }

    @Test
    fun `getEmail should return null for invalid token`() {
        val email = jwtService.getEmail("invalid.token")

        assertNull(email)
    }

    @Test
    fun `getTokenType should return null for invalid token`() {
        val tokenType = jwtService.getTokenType("invalid.token")

        assertNull(tokenType)
    }

    // ==================== Token Expiration Tests ====================

    @Test
    fun `isTokenExpired should return true for invalid token`() {
        assertTrue(jwtService.isTokenExpired("invalid.token"))
    }

    @Test
    fun `access and refresh tokens should be different`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val accessToken = jwtService.generateAccessToken(userId, email)
        val refreshToken = jwtService.generateRefreshToken(userId)

        assertNotEquals(accessToken, refreshToken)
    }

    @Test
    fun `multiple tokens for same user should be different`() {
        val userId = UUID.randomUUID().toString()
        val email = "test@example.com"

        val token1 = jwtService.generateAccessToken(userId, email)
        Thread.sleep(10) // Ensure different timestamp
        val token2 = jwtService.generateAccessToken(userId, email)

        assertNotEquals(token1, token2)
    }
}
