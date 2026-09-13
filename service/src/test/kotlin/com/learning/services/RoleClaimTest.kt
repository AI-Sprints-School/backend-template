package com.learning.services

import at.favre.lib.crypto.bcrypt.BCrypt
import com.learning.domain.models.Roles
import com.learning.domain.models.User
import com.learning.models.LoginRequest
import com.learning.repositories.UserRepository
import com.learning.security.AuthService
import com.learning.security.JwtService
import com.learning.security.JwtSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Роль в access-токене: claim `role` выпускается JwtService и выставляется
 * AuthService по роли пользователя из базы.
 */
@Tag("core")
@Tag("chapter5")
class RoleClaimTest {

    private val jwt = JwtService(JwtSettings("test-secret-key-for-role-claims-32b", "issuer", "audience", "realm", 3600, 604800))

    @Test
    fun `access token should carry student role by default`() {
        val token = jwt.generateAccessToken(UUID.randomUUID().toString(), "a@example.com")

        assertEquals(Roles.STUDENT, jwt.getRole(token))
    }

    @Test
    fun `access token should carry given role`() {
        val token = jwt.generateAccessToken(UUID.randomUUID().toString(), "a@example.com", Roles.ADMIN)

        assertEquals(Roles.ADMIN, jwt.getRole(token))
    }

    @Test
    fun `refresh token should not carry role`() {
        val token = jwt.generateRefreshToken(UUID.randomUUID().toString())

        assertNull(jwt.getRole(token))
    }

    @Test
    fun `login should issue access token with role of user from database`() {
        val password = "Password123"
        val admin = User(
            id = UUID.randomUUID(),
            email = "admin@example.com",
            passwordHash = BCrypt.withDefaults().hashToString(4, password.toCharArray()),
            firstName = "Админ",
            lastName = "Школы",
            role = Roles.ADMIN,
        )
        val users = mockk<UserRepository>(relaxed = true)
        every { users.findByEmail(admin.email) } returns admin
        every { users.getPasswordHash(admin.email) } returns admin.passwordHash

        val response = AuthService(jwt, users).login(LoginRequest(admin.email, password)).getOrThrow()

        assertEquals(Roles.ADMIN, jwt.getRole(response.accessToken))
    }
}
