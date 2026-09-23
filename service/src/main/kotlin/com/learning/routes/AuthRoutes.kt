package com.learning.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.learning.models.*
import com.learning.repositories.TokenRepository
import com.learning.repositories.UserRepository
import com.learning.security.AuthService
import com.learning.services.EmailService
import com.learning.utils.*
import com.learning.validation.PasswordPolicy
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.util.*

fun Route.authRoutes(
    authService: AuthService,
    tokenRepository: TokenRepository,
    userRepository: UserRepository,
    emailService: EmailService,
) {

    route("/auth") {
        rateLimit(RateLimitName("auth")) {
            /**
             * POST /api/v1/auth/register
             * Регистрация нового пользователя
             */
            post("/register") {
                TODO("Глава 5, урок 19: POST /auth/register")
            }

            /**
             * POST /api/v1/auth/login
             * Вход пользователя
             */
            post("/login") {
                TODO("Глава 5, урок 21: POST /auth/login")
            }

            /**
             * POST /api/v1/auth/refresh
             * Обновление access токена
             */
            post("/refresh") {
                TODO("Глава 5, урок 21: POST /auth/refresh")
            }

            /**
             * POST /api/v1/auth/logout
             * Выход пользователя
             */
            post("/logout") {
                TODO("Глава 5, урок 21: POST /auth/logout")
            }

            /**
             * POST /api/v1/auth/verify-email
             * Подтверждение email адреса
             */
            post("/verify-email") {
                call.safeExecute {
                    val request = call.receive<VerifyEmailRequest>()

                    if (request.token.isBlank()) {
                        throw AppException.ValidationError("Токен обязателен")
                    }

                    val userId = tokenRepository.findUserIdByEmailVerificationToken(request.token)
                        ?: throw AppException.BadRequestError("Невалидный или просроченный токен")

                    // Обновляем статус верификации
                    val updated = userRepository.updateEmailVerification(userId, true)
                    if (!updated) {
                        throw AppException.DatabaseError("Не удалось обновить статус верификации")
                    }

                    // Удаляем использованный токен
                    tokenRepository.deleteEmailVerificationToken(request.token)

                    call.respond(
                        HttpStatusCode.OK,
                        MessageResponse(message = "Email успешно подтвержден")
                    )
                }
            }

            /**
             * GET /api/v1/auth/verify-email
             * Подтверждение email адреса (для ссылки из email)
             */
            get("/verify-email") {
                call.safeExecute {
                    val token = call.request.queryParameters["token"]

                    if (token.isNullOrBlank()) {
                        throw AppException.ValidationError("Токен обязателен")
                    }

                    val userId = tokenRepository.findUserIdByEmailVerificationToken(token)
                        ?: throw AppException.BadRequestError("Невалидный или просроченный токен")

                    // Обновляем статус верификации
                    val updated = userRepository.updateEmailVerification(userId, true)
                    if (!updated) {
                        throw AppException.DatabaseError("Не удалось обновить статус верификации")
                    }

                    // Удаляем использованный токен
                    tokenRepository.deleteEmailVerificationToken(token)

                    call.respond(
                        HttpStatusCode.OK,
                        MessageResponse(message = "Email успешно подтвержден")
                    )
                }
            }

            /**
             * POST /api/v1/auth/password/reset-request
             * Запрос на сброс пароля
             */
            post("/password/reset-request") {
                call.safeExecute {
                    val request = call.receive<PasswordResetRequestModel>()

                    if (request.email.isBlank()) {
                        throw AppException.ValidationError("Email обязателен")
                    }

                    // Всегда возвращаем успех для безопасности (не раскрываем существование email)
                    val user = userRepository.findByEmail(request.email)

                    if (user != null) {
                        // Создаем токен сброса пароля
                        val resetToken = tokenRepository.generateSecureToken()
                        val expiresAt = Instant.now().plusSeconds(3600) // 1 час

                        tokenRepository.createPasswordResetToken(
                            userId = user.id,
                            token = resetToken,
                            expiresAt = expiresAt
                        )

                        // Отправляем email
                        emailService.sendPasswordResetEmail(
                            to = user.email,
                            token = resetToken,
                            firstName = user.firstName
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        MessageResponse(message = "Если email зарегистрирован, инструкции отправлены на почту")
                    )
                }
            }

            /**
             * POST /api/v1/auth/password/reset
             * Сброс пароля с токеном
             */
            post("/password/reset") {
                call.safeExecute {
                    val request = call.receive<PasswordResetConfirmRequest>()

                    if (request.token.isBlank()) {
                        throw AppException.ValidationError("Токен обязателен")
                    }

                    if (!PasswordPolicy.isValid(request.newPassword)) {
                        throw AppException.ValidationError(PasswordPolicy.MESSAGE)
                    }

                    val userId = tokenRepository.findUserIdByPasswordResetToken(request.token)
                        ?: throw AppException.BadRequestError("Невалидный или просроченный токен")

                    // Хешируем новый пароль
                    val passwordHash = BCrypt.withDefaults().hashToString(12, request.newPassword.toCharArray())

                    // Обновляем пароль
                    val updated = userRepository.updatePasswordHash(userId, passwordHash)
                    if (!updated) {
                        throw AppException.DatabaseError("Не удалось обновить пароль")
                    }

                    // Отмечаем токен как использованный
                    tokenRepository.markPasswordResetTokenAsUsed(request.token)

                    // Отзываем все refresh tokens пользователя для безопасности
                    tokenRepository.revokeAllUserRefreshTokens(userId)

                    call.respond(
                        HttpStatusCode.OK,
                        MessageResponse(message = "Пароль успешно изменен")
                    )
                }
            }
        }

        /**
         * GET /api/v1/auth/me
         * Получение информации о текущем пользователе (требует авторизации)
         */
        rateLimit(RateLimitName("api")) {
            authenticate("auth-jwt") {
                get("/me") {
                    TODO("Глава 5, урок 22: GET /auth/me")
                }
            }
        }
    }
}

