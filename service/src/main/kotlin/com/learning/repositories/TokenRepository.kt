package com.learning.repositories

import com.learning.database.EmailVerificationTokens
import com.learning.database.PasswordResetTokens
import com.learning.database.RefreshTokens
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Instant
import java.util.*

class TokenRepository {
    private val logger = LoggerFactory.getLogger(TokenRepository::class.java)

    // ==================== Email Verification Tokens ====================

    fun createEmailVerificationToken(userId: UUID, token: String, expiresAt: Instant): Boolean {
        return try {
            transaction {
                // Удаляем старые токены для этого пользователя
                EmailVerificationTokens.deleteWhere { EmailVerificationTokens.userId eq userId }

                // Создаем новый
                EmailVerificationTokens.insert {
                    it[id] = UUID.randomUUID()
                    it[EmailVerificationTokens.userId] = userId
                    it[EmailVerificationTokens.token] = token
                    it[EmailVerificationTokens.expiresAt] = expiresAt
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Ошибка создания email verification token: ${e.message}", e)
            false
        }
    }

    fun findUserIdByEmailVerificationToken(token: String): UUID? {
        return try {
            transaction {
                EmailVerificationTokens
                    .selectAll().where { 
                        (EmailVerificationTokens.token eq token) and 
                        (EmailVerificationTokens.expiresAt greater Instant.now()) 
                    }
                    .singleOrNull()
                    ?.get(EmailVerificationTokens.userId)
            }
        } catch (e: Exception) {
            logger.error("Ошибка поиска email verification token: ${e.message}", e)
            null
        }
    }

    fun deleteEmailVerificationToken(token: String): Boolean {
        return try {
            transaction {
                EmailVerificationTokens.deleteWhere { EmailVerificationTokens.token eq token } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка удаления email verification token: ${e.message}", e)
            false
        }
    }

    fun deleteExpiredEmailVerificationTokens(): Int {
        return try {
            transaction {
                EmailVerificationTokens.deleteWhere { EmailVerificationTokens.expiresAt less Instant.now() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка удаления expired email verification tokens: ${e.message}", e)
            0
        }
    }

    // ==================== Password Reset Tokens ====================

    fun createPasswordResetToken(userId: UUID, token: String, expiresAt: Instant): Boolean {
        return try {
            transaction {
                // Удаляем старые неиспользованные токены для этого пользователя
                PasswordResetTokens.deleteWhere { 
                    (PasswordResetTokens.userId eq userId) and 
                    PasswordResetTokens.usedAt.isNull()
                }

                // Создаем новый
                PasswordResetTokens.insert {
                    it[id] = UUID.randomUUID()
                    it[PasswordResetTokens.userId] = userId
                    it[PasswordResetTokens.token] = token
                    it[PasswordResetTokens.expiresAt] = expiresAt
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Ошибка создания password reset token: ${e.message}", e)
            false
        }
    }

    fun findUserIdByPasswordResetToken(token: String): UUID? {
        return try {
            transaction {
                PasswordResetTokens
                    .selectAll().where { 
                        (PasswordResetTokens.token eq token) and 
                        (PasswordResetTokens.expiresAt greater Instant.now()) and
                        PasswordResetTokens.usedAt.isNull()
                    }
                    .singleOrNull()
                    ?.get(PasswordResetTokens.userId)
            }
        } catch (e: Exception) {
            logger.error("Ошибка поиска password reset token: ${e.message}", e)
            null
        }
    }

    fun markPasswordResetTokenAsUsed(token: String): Boolean {
        return try {
            transaction {
                PasswordResetTokens.update({ PasswordResetTokens.token eq token }) {
                    it[usedAt] = Instant.now()
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка обновления password reset token: ${e.message}", e)
            false
        }
    }

    fun deleteExpiredPasswordResetTokens(): Int {
        return try {
            transaction {
                PasswordResetTokens.deleteWhere { PasswordResetTokens.expiresAt less Instant.now() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка удаления expired password reset tokens: ${e.message}", e)
            0
        }
    }

    // ==================== Refresh Tokens ====================
    //
    // В базе лежит не сам refresh-токен, а его SHA-256 (64 hex-символа в столбце token):
    // утечка дампа таблицы не даёт готовых сессий. Токен длинный и случайный, поэтому
    // соль и медленный хеш, как у пароля, не нужны; поиск идёт по хешу через уникальный
    // индекс столбца token (V2). Снаружи функции по-прежнему принимают токен как есть.

    fun createRefreshToken(userId: UUID, token: String, expiresAt: Instant): Boolean {
        return try {
            transaction {
                RefreshTokens.insert {
                    it[id] = UUID.randomUUID()
                    it[RefreshTokens.userId] = userId
                    it[RefreshTokens.token] = hashToken(token)
                    it[RefreshTokens.expiresAt] = expiresAt
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Ошибка создания refresh token: ${e.message}", e)
            false
        }
    }

    fun findUserIdByRefreshToken(token: String): UUID? {
        return try {
            transaction {
                RefreshTokens
                    .selectAll().where { 
                        (RefreshTokens.token eq hashToken(token)) and 
                        (RefreshTokens.expiresAt greater Instant.now()) and
                        (RefreshTokens.isRevoked eq false)
                    }
                    .singleOrNull()
                    ?.get(RefreshTokens.userId)
            }
        } catch (e: Exception) {
            logger.error("Ошибка поиска refresh token: ${e.message}", e)
            null
        }
    }

    fun revokeRefreshToken(token: String): Boolean {
        return try {
            transaction {
                RefreshTokens.update({ RefreshTokens.token eq hashToken(token) }) {
                    it[isRevoked] = true
                } > 0
            }
        } catch (e: Exception) {
            logger.error("Ошибка отзыва refresh token: ${e.message}", e)
            false
        }
    }

    fun revokeAllUserRefreshTokens(userId: UUID): Int {
        return try {
            transaction {
                RefreshTokens.update({ 
                    (RefreshTokens.userId eq userId) and 
                    (RefreshTokens.isRevoked eq false) 
                }) {
                    it[isRevoked] = true
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка отзыва всех refresh tokens пользователя: ${e.message}", e)
            0
        }
    }

    fun deleteExpiredRefreshTokens(): Int {
        return try {
            transaction {
                RefreshTokens.deleteWhere { RefreshTokens.expiresAt less Instant.now() }
            }
        } catch (e: Exception) {
            logger.error("Ошибка удаления expired refresh tokens: ${e.message}", e)
            0
        }
    }

    // ==================== Utility ====================

    private fun hashToken(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)).toHexString()

    fun generateSecureToken(): String {
        return UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "")
    }
}
