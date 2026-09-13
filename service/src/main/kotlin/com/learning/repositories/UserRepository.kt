package com.learning.repositories

import com.learning.database.Users
import com.learning.domain.models.Roles
import com.learning.domain.models.User
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class UserRepository {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    fun createUser(email: String, passwordHash: String, firstName: String, lastName: String): User? {
        TODO("Глава 3, урок 11: UserRepository.createUser")
    }

    fun findByEmail(email: String): User? {
        TODO("Глава 3, урок 11: UserRepository.findByEmail")
    }

    fun findById(userId: UUID): User? {
        TODO("Глава 3, урок 11: UserRepository.findById")
    }

    fun getPasswordHash(email: String): String? {
        TODO("Глава 3, урок 11: UserRepository.getPasswordHash")
    }

    fun updatePasswordHash(userId: UUID, passwordHash: String): Boolean {
        return try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[Users.passwordHash] = passwordHash
                    it[Users.updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при обновлении пароля: ${e.message}", e)
            false
        }
    }

    fun updateEmailVerification(userId: UUID, isVerified: Boolean): Boolean {
        TODO("Глава 3, урок 11: UserRepository.updateEmailVerification")
    }

    fun setPasswordResetToken(userId: UUID, token: String, expiresAt: Instant): Boolean {
        return try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[Users.passwordResetToken] = token
                    it[Users.passwordResetExpires] = expiresAt
                    it[Users.updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при установке токена сброса пароля: ${e.message}", e)
            false
        }
    }

    fun clearPasswordResetToken(userId: UUID): Boolean {
        return try {
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[Users.passwordResetToken] = null
                    it[Users.passwordResetExpires] = null
                    it[Users.updatedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка при очистке токена сброса пароля: ${e.message}", e)
            false
        }
    }

    fun findByPasswordResetToken(token: String): User? {
        return try {
            transaction {
                Users.selectAll().where {
                    Users.passwordResetToken eq token and
                            (Users.passwordResetExpires greater Instant.now())
                }.singleOrNull()?.toUser()
            }
        } catch (e: Exception) {
            logger.error("Ошибка при поиске пользователя по токену сброса пароля: ${e.message}", e)
            null
        }
    }

    fun updateAvatar(userId: UUID, avatarUrl: String): Boolean {
        TODO("Глава 3, урок 11: UserRepository.updateAvatar")
    }

    /** Меняет роль. Роль в уже выданных токенах не меняется — нужен новый вход. */
    fun updateRole(userId: UUID, role: String): Boolean {
        TODO("Глава 3, урок 11: UserRepository.updateRole")
    }

    /** Обновляет имя, фамилию и почту; `null` — поле не трогать. */
    fun updateProfile(userId: UUID, firstName: String?, lastName: String?, email: String?): Boolean {
        TODO("Глава 3, урок 11: UserRepository.updateProfile")
    }

    private fun ResultRow.toUser(): User {
        return User(
            id = this[Users.id],
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            firstName = this[Users.firstName],
            lastName = this[Users.lastName],
            isEmailVerified = this[Users.isEmailVerified],
            emailVerificationToken = this[Users.emailVerificationToken],
            passwordResetToken = this[Users.passwordResetToken],
            passwordResetExpires = this[Users.passwordResetExpires],
            googleId = this[Users.googleId],
            avatarUrl = this[Users.avatarUrl],
            createdAt = this[Users.createdAt],
            updatedAt = this[Users.updatedAt],
            role = this[Users.role]
        )
    }
}

