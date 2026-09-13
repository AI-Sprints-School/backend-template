package com.learning.integration

import org.junit.jupiter.api.Tag
import com.learning.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Instant
import java.util.*

/**
 * Интеграционные тесты для UserRepository с реальной PostgreSQL
 */
@Tag("core")
@Tag("chapter3")
class UserRepositoryIntegrationTest : DatabaseTestBase() {

    private val userRepository = UserRepository()

    // ==================== Create User Tests ====================

    @Test
    fun `createUser should create user and return it`() {
        val email = "test@example.com"
        val passwordHash = "hashed_password"
        val firstName = "Иван"
        val lastName = "Иванов"

        val user = userRepository.createUser(email, passwordHash, firstName, lastName)

        assertNotNull(user)
        assertEquals(email, user?.email)
        assertEquals(firstName, user?.firstName)
        assertEquals(lastName, user?.lastName)
        assertNotNull(user?.id)
    }

    @Test
    fun `createUser should generate unique UUID`() {
        val user1 = userRepository.createUser("user1@example.com", "hash1", "User", "One")
        val user2 = userRepository.createUser("user2@example.com", "hash2", "User", "Two")

        assertNotNull(user1)
        assertNotNull(user2)
        assertNotEquals(user1?.id, user2?.id)
    }

    @Test
    fun `createUser should set timestamps`() {
        val beforeCreate = Instant.now().minusSeconds(1)

        val user = userRepository.createUser("test@example.com", "hash", "Test", "User")

        assertNotNull(user)
        assertTrue(user!!.createdAt.isAfter(beforeCreate))
        assertTrue(user.updatedAt.isAfter(beforeCreate))
    }

    // ==================== Find By Email Tests ====================

    @Test
    fun `findByEmail should return user when exists`() {
        val email = "findme@example.com"
        userRepository.createUser(email, "hash", "Find", "Me")

        val found = userRepository.findByEmail(email)

        assertNotNull(found)
        assertEquals(email, found?.email)
    }

    @Test
    fun `findByEmail should return null when not exists`() {
        val found = userRepository.findByEmail("nonexistent@example.com")

        assertNull(found)
    }

    @Test
    fun `findByEmail should be case sensitive`() {
        userRepository.createUser("Test@Example.com", "hash", "Test", "User")

        val foundUpper = userRepository.findByEmail("TEST@EXAMPLE.COM")
        val foundLower = userRepository.findByEmail("test@example.com")
        val foundOriginal = userRepository.findByEmail("Test@Example.com")

        assertNull(foundUpper)
        assertNull(foundLower)
        assertNotNull(foundOriginal)
    }

    // ==================== Find By Id Tests ====================

    @Test
    fun `findById should return user when exists`() {
        val created = userRepository.createUser("byid@example.com", "hash", "By", "Id")
        assertNotNull(created)

        val found = userRepository.findById(created!!.id)

        assertNotNull(found)
        assertEquals(created.id, found?.id)
        assertEquals(created.email, found?.email)
    }

    @Test
    fun `findById should return null when not exists`() {
        val found = userRepository.findById(UUID.randomUUID())

        assertNull(found)
    }

    // ==================== Get Password Hash Tests ====================

    @Test
    fun `getPasswordHash should return hash when user exists`() {
        val email = "password@example.com"
        val passwordHash = "secure_hash_123"
        userRepository.createUser(email, passwordHash, "Pass", "Word")

        val hash = userRepository.getPasswordHash(email)

        assertEquals(passwordHash, hash)
    }

    @Test
    fun `getPasswordHash should return null when user not exists`() {
        val hash = userRepository.getPasswordHash("nonexistent@example.com")

        assertNull(hash)
    }

    // ==================== Update Password Hash Tests ====================

    @Test
    fun `updatePasswordHash should update hash`() {
        val user = userRepository.createUser("update@example.com", "old_hash", "Update", "User")
        assertNotNull(user)

        val newHash = "new_hash_456"
        val success = userRepository.updatePasswordHash(user!!.id, newHash)

        assertTrue(success)
        val hash = userRepository.getPasswordHash(user.email)
        assertEquals(newHash, hash)
    }

    @Test
    fun `updatePasswordHash should return false when user not exists`() {
        val success = userRepository.updatePasswordHash(UUID.randomUUID(), "new_hash")

        assertFalse(success)
    }

    // ==================== Email Verification Tests ====================

    @Test
    fun `updateEmailVerification should update status`() {
        val user = userRepository.createUser("verify@example.com", "hash", "Verify", "User")
        assertNotNull(user)
        assertFalse(user!!.isEmailVerified)

        val success = userRepository.updateEmailVerification(user.id, true)

        assertTrue(success)
        val updated = userRepository.findById(user.id)
        assertTrue(updated!!.isEmailVerified)
    }

    // ==================== Password Reset Token Tests ====================

    @Test
    fun `setPasswordResetToken should set token and expiration`() {
        val user = userRepository.createUser("reset@example.com", "hash", "Reset", "User")
        assertNotNull(user)

        val token = "reset_token_123"
        val expiresAt = Instant.now().plusSeconds(3600)

        val success = userRepository.setPasswordResetToken(user!!.id, token, expiresAt)

        assertTrue(success)
        val updated = userRepository.findById(user.id)
        assertEquals(token, updated?.passwordResetToken)
        assertNotNull(updated?.passwordResetExpires)
    }

    @Test
    fun `clearPasswordResetToken should clear token`() {
        val user = userRepository.createUser("clear@example.com", "hash", "Clear", "User")
        assertNotNull(user)
        userRepository.setPasswordResetToken(user!!.id, "token", Instant.now().plusSeconds(3600))

        val success = userRepository.clearPasswordResetToken(user.id)

        assertTrue(success)
        val updated = userRepository.findById(user.id)
        assertNull(updated?.passwordResetToken)
        assertNull(updated?.passwordResetExpires)
    }

    @Test
    fun `findByPasswordResetToken should return user when token valid`() {
        val user = userRepository.createUser("tokenuser@example.com", "hash", "Token", "User")
        assertNotNull(user)

        val token = "valid_token"
        val expiresAt = Instant.now().plusSeconds(3600)
        userRepository.setPasswordResetToken(user!!.id, token, expiresAt)

        val found = userRepository.findByPasswordResetToken(token)

        assertNotNull(found)
        assertEquals(user.id, found?.id)
    }

    @Test
    fun `findByPasswordResetToken should return null when token expired`() {
        val user = userRepository.createUser("expired@example.com", "hash", "Expired", "User")
        assertNotNull(user)

        val token = "expired_token"
        val expiresAt = Instant.now().minusSeconds(3600) // Already expired
        userRepository.setPasswordResetToken(user!!.id, token, expiresAt)

        val found = userRepository.findByPasswordResetToken(token)

        assertNull(found)
    }

    @Test
    fun `findByPasswordResetToken should return null when token not exists`() {
        val found = userRepository.findByPasswordResetToken("nonexistent_token")

        assertNull(found)
    }

    // ==================== Update Avatar Tests ====================

    @Test
    fun `updateAvatar should update avatar url`() {
        val user = userRepository.createUser("avatar@example.com", "hash", "Avatar", "User")
        assertNotNull(user)
        assertNull(user?.avatarUrl)

        val avatarUrl = "https://example.com/avatar.jpg"
        val success = userRepository.updateAvatar(user!!.id, avatarUrl)

        assertTrue(success)
        val updated = userRepository.findById(user.id)
        assertEquals(avatarUrl, updated?.avatarUrl)
    }
}
