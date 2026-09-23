package com.learning.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.learning.models.*
import com.learning.repositories.TokenRepository
import com.learning.repositories.UserRepository
import com.learning.utils.AppException
import com.learning.validation.PasswordPolicy
import com.learning.validation.Validators
import java.util.*
import java.time.Instant

/**
 * Сервис аутентификации с поддержкой базы данных
 */
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    /**
     * Refresh-токены хранятся в таблице `refresh_tokens` (перенос с `last-checkpoints`, 04df904).
     * До переноса они жили в памяти процесса: отзыв при сбросе пароля
     * (`revokeAllUserRefreshTokens`) ничего не отзывал, а перезапуск разлогинивал всех.
     * Значение по умолчанию — для юнит-тестов на моках, в приложении зависимость приходит из DI.
     */
    private val tokenRepository: TokenRepository = TokenRepository(),
) {
    
    /**
     * Регистрация нового пользователя
     */
    fun register(request: RegisterRequest): Result<AuthResponse> {
        TODO("Глава 5, урок 19: регистрация, BCrypt, дубликат почты")
    }
    
    /**
     * Вход пользователя
     */
    fun login(request: LoginRequest): Result<AuthResponse> {
        TODO("Глава 5, урок 21: вход")
    }
    
    /**
     * Обновление access токена через refresh токен
     */
    fun refreshAccessToken(request: RefreshTokenRequest): Result<AuthResponse> {
        TODO("Глава 5, урок 21: обновление токена; refresh не принимается как access")
    }
    
    /**
     * Выход пользователя (удаление refresh токена)
     */
    fun logout(refreshToken: String): Result<Unit> {
        TODO("Глава 5, урок 21: выход")
    }
    
    /**
     * Получение пользователя по ID
     */
    fun getUserById(userId: String): com.learning.domain.models.User? {
        TODO("Глава 5, урок 22: пользователь по id")
    }
    
    /**
     * Получение пользователя по email
     */
    fun getUserByEmail(email: String): com.learning.domain.models.User? {
        TODO("Глава 5, урок 22: пользователь по почте")
    }
    
    /**
     * Сохранение refresh токена
     */
    private fun saveRefreshToken(userId: String, token: String) {
        TODO("Глава 5, урок 21: хранение refresh-токена")
    }

    
    /**
     * Валидация email
     */
    private fun isValidEmail(email: String): Boolean = TODO("Глава 5, урок 19: проверка формата почты")
}
