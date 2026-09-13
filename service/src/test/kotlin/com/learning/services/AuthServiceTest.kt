package com.learning.services

import org.junit.jupiter.api.Tag
import at.favre.lib.crypto.bcrypt.BCrypt
import com.learning.domain.models.User
import com.learning.models.LoginRequest
import com.learning.models.RefreshTokenRequest
import com.learning.models.RegisterRequest
import com.learning.repositories.UserRepository
import com.learning.security.AuthService
import com.learning.security.JwtService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.*

/**
 * Unit тесты для AuthService
 */
@Tag("core")
@Tag("chapter5")
class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var jwtService: JwtService
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        jwtService = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        authService = AuthService(jwtService, userRepository)
    }

    // ==================== Registration Tests ====================

    @Test
    fun `register should succeed with valid data`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        val userId = UUID.randomUUID()
        val user = createTestUser(userId, request.email, request.firstName, request.lastName)

        every { userRepository.findByEmail(request.email) } returns null
        every { userRepository.createUser(any(), any(), any(), any()) } returns user
        every { jwtService.generateAccessToken(any(), any()) } returns "access_token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh_token"

        val result = authService.register(request)

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("access_token", response?.accessToken)
        assertEquals("refresh_token", response?.refreshToken)
        assertEquals(request.email, response?.user?.email)
    }

    @Test
    fun `register should fail when email already exists`() {
        val request = RegisterRequest(
            email = "existing@example.com",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        val existingUser = createTestUser(UUID.randomUUID(), request.email, "Existing", "User")
        every { userRepository.findByEmail(request.email) } returns existingUser

        val result = authService.register(request)

        assertTrue(result.isFailure)
        assertEquals("Пользователь с таким email уже существует", result.exceptionOrNull()?.message)
    }

    @Test
    fun `register should fail with invalid email format`() {
        val request = RegisterRequest(
            email = "invalid-email",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        every { userRepository.findByEmail(any()) } returns null

        val result = authService.register(request)

        assertTrue(result.isFailure)
        assertEquals("Неверный формат email", result.exceptionOrNull()?.message)
    }

    @Test
    fun `register should fail with short password`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "12345",
            firstName = "Иван",
            lastName = "Иванов"
        )

        every { userRepository.findByEmail(any()) } returns null

        val result = authService.register(request)

        assertTrue(result.isFailure)
        assertEquals("Пароль должен содержать минимум 6 символов", result.exceptionOrNull()?.message)
    }

    @Test
    fun `register should fail when user creation fails`() {
        val request = RegisterRequest(
            email = "test@example.com",
            password = "Password123",
            firstName = "Иван",
            lastName = "Иванов"
        )

        every { userRepository.findByEmail(any()) } returns null
        every { userRepository.createUser(any(), any(), any(), any()) } returns null

        val result = authService.register(request)

        assertTrue(result.isFailure)
        assertEquals("Не удалось создать пользователя", result.exceptionOrNull()?.message)
    }

    // ==================== Login Tests ====================

    @Test
    fun `login should succeed with valid credentials`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "Password123"
        )

        val userId = UUID.randomUUID()
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        val user = createTestUser(userId, request.email, "Иван", "Иванов", passwordHash)

        every { userRepository.findByEmail(request.email) } returns user
        every { userRepository.getPasswordHash(request.email) } returns passwordHash
        every { jwtService.generateAccessToken(any(), any()) } returns "access_token"
        every { jwtService.generateRefreshToken(any()) } returns "refresh_token"

        val result = authService.login(request)

        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals("access_token", response?.accessToken)
        assertEquals("refresh_token", response?.refreshToken)
    }

    @Test
    fun `login should fail when user not found`() {
        val request = LoginRequest(
            email = "nonexistent@example.com",
            password = "Password123"
        )

        every { userRepository.findByEmail(request.email) } returns null

        val result = authService.login(request)

        assertTrue(result.isFailure)
        assertEquals("Неверный email или пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `login should fail with wrong password`() {
        val request = LoginRequest(
            email = "test@example.com",
            password = "WrongPassword"
        )

        val userId = UUID.randomUUID()
        val correctPasswordHash = BCrypt.withDefaults().hashToString(12, "CorrectPassword".toCharArray())
        val user = createTestUser(userId, request.email, "Иван", "Иванов", correctPasswordHash)

        every { userRepository.findByEmail(request.email) } returns user
        every { userRepository.getPasswordHash(request.email) } returns correctPasswordHash

        val result = authService.login(request)

        assertTrue(result.isFailure)
        assertEquals("Неверный email или пароль", result.exceptionOrNull()?.message)
    }

    // ==================== Refresh Token Tests ====================

    @Test
    fun `refreshAccessToken should fail with invalid token`() {
        val request = RefreshTokenRequest(refreshToken = "invalid_token")

        every { jwtService.validateToken(any()) } returns false

        val result = authService.refreshAccessToken(request)

        assertTrue(result.isFailure)
        assertEquals("Недействительный refresh токен", result.exceptionOrNull()?.message)
    }

    @Test
    fun `refreshAccessToken should fail with access token instead of refresh`() {
        val request = RefreshTokenRequest(refreshToken = "some_token")

        every { jwtService.validateToken(any()) } returns true
        every { jwtService.getTokenType(any()) } returns "access"

        val result = authService.refreshAccessToken(request)

        assertTrue(result.isFailure)
        assertEquals("Неверный тип токена", result.exceptionOrNull()?.message)
    }

    // ==================== Logout Tests ====================

    @Test
    fun `logout should always succeed`() {
        val result = authService.logout("any_token")

        assertTrue(result.isSuccess)
    }

    // ==================== Get User Tests ====================

    @Test
    fun `getUserById should return user when found`() {
        val userId = UUID.randomUUID()
        val user = createTestUser(userId, "test@example.com", "Иван", "Иванов")

        every { userRepository.findById(userId) } returns user

        val result = authService.getUserById(userId.toString())

        assertNotNull(result)
        assertEquals(userId, result?.id)
    }

    @Test
    fun `getUserById should return null when not found`() {
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns null

        val result = authService.getUserById(userId.toString())

        assertNull(result)
    }

    @Test
    fun `getUserById should return null for invalid UUID`() {
        val result = authService.getUserById("not-a-uuid")

        assertNull(result)
    }

    @Test
    fun `getUserByEmail should return user when found`() {
        val email = "test@example.com"
        val user = createTestUser(UUID.randomUUID(), email, "Иван", "Иванов")

        every { userRepository.findByEmail(email) } returns user

        val result = authService.getUserByEmail(email)

        assertNotNull(result)
        assertEquals(email, result?.email)
    }

    @Test
    fun `getUserByEmail should return null when not found`() {
        val email = "nonexistent@example.com"

        every { userRepository.findByEmail(email) } returns null

        val result = authService.getUserByEmail(email)

        assertNull(result)
    }

    // ==================== Helper Functions ====================

    private fun createTestUser(
        id: UUID,
        email: String,
        firstName: String,
        lastName: String,
        passwordHash: String = "hashed_password"
    ): User {
        return User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            firstName = firstName,
            lastName = lastName,
            isEmailVerified = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
