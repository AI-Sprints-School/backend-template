package com.learning.utils

import com.learning.models.ErrorResponse
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

/**
 * Типы ошибок API
 */
object ErrorTypes {
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR"
    const val AUTHORIZATION_ERROR = "AUTHORIZATION_ERROR"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONFLICT = "CONFLICT"
    const val BAD_REQUEST = "BAD_REQUEST"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
    const val DATABASE_ERROR = "DATABASE_ERROR"
    const val RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED"
}

/**
 * Пользовательское исключение для бизнес-логики
 */
sealed class AppException(
    message: String,
    val errorType: String,
    val statusCode: HttpStatusCode = HttpStatusCode.BadRequest
) : Exception(message) {
    
    /**
     * Ошибка валидации данных
     */
    class ValidationError(message: String) : AppException(
        message = message,
        errorType = ErrorTypes.VALIDATION_ERROR,
        statusCode = HttpStatusCode.BadRequest
    )
    
    /**
     * Ошибка аутентификации
     */
    class AuthenticationError(message: String = "Неверные учетные данные") : AppException(
        message = message,
        errorType = ErrorTypes.AUTHENTICATION_ERROR,
        statusCode = HttpStatusCode.Unauthorized
    )
    
    /**
     * Ошибка авторизации
     */
    class AuthorizationError(message: String = "Недостаточно прав доступа") : AppException(
        message = message,
        errorType = ErrorTypes.AUTHORIZATION_ERROR,
        statusCode = HttpStatusCode.Forbidden
    )
    
    /**
     * Ресурс не найден
     */
    class NotFoundError(message: String = "Ресурс не найден") : AppException(
        message = message,
        errorType = ErrorTypes.NOT_FOUND,
        statusCode = HttpStatusCode.NotFound
    )
    
    /**
     * Конфликт данных (например, email уже существует)
     */
    class ConflictError(message: String) : AppException(
        message = message,
        errorType = ErrorTypes.CONFLICT,
        statusCode = HttpStatusCode.Conflict
    )
    
    /**
     * Некорректный запрос
     */
    class BadRequestError(message: String) : AppException(
        message = message,
        errorType = ErrorTypes.BAD_REQUEST,
        statusCode = HttpStatusCode.BadRequest
    )
    
    /**
     * Ошибка базы данных
     */
    class DatabaseError(message: String = "Ошибка базы данных") : AppException(
        message = message,
        errorType = ErrorTypes.DATABASE_ERROR,
        statusCode = HttpStatusCode.InternalServerError
    )
}

/**
 * Extension функция для обработки исключений в роутах
 */
suspend fun ApplicationCall.handleException(exception: Throwable) {
    val logger = LoggerFactory.getLogger("ErrorHandling")
    
    when (exception) {
        is RequestValidationException -> {
            val details = exception.reasons.joinToString("; ")
            logger.warn("Ошибка валидации: $details")
            respondWithError(
                statusCode = HttpStatusCode.BadRequest,
                error = ErrorTypes.VALIDATION_ERROR,
                message = "Ошибка валидации данных",
                details = details
            )
        }
        
        is AppException -> {
            logger.warn("${exception.errorType}: ${exception.message}")
            respondWithError(
                statusCode = exception.statusCode,
                error = exception.errorType,
                message = exception.message ?: "Произошла ошибка",
                details = null
            )
        }
        
        is BadRequestException -> {
            logger.warn("Некорректный запрос: ${exception.message}")
            respondWithError(
                statusCode = HttpStatusCode.BadRequest,
                error = ErrorTypes.BAD_REQUEST,
                message = exception.message ?: "Некорректный запрос",
                details = null
            )
        }
        
        is NotFoundException -> {
            logger.warn("Не найдено: ${exception.message}")
            respondWithError(
                statusCode = HttpStatusCode.NotFound,
                error = ErrorTypes.NOT_FOUND,
                message = exception.message ?: "Ресурс не найден",
                details = null
            )
        }
        
        else -> {
            logger.error("Внутренняя ошибка сервера", exception)
            respondWithError(
                statusCode = HttpStatusCode.InternalServerError,
                error = ErrorTypes.INTERNAL_ERROR,
                message = "Внутренняя ошибка сервера",
                details = if (application.developmentMode) exception.message else null
            )
        }
    }
}

/**
 * Extension функция для отправки ответа с ошибкой
 */
suspend fun ApplicationCall.respondWithError(
    statusCode: HttpStatusCode,
    error: String,
    message: String,
    details: String? = null
) {
    respond(
        status = statusCode,
        message = ErrorResponse(
            error = error,
            message = message,
            details = details
        )
    )
}

/**
 * Extension функция для безопасного выполнения блока кода с обработкой ошибок
 */
suspend fun <T> ApplicationCall.safeExecute(block: suspend () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        handleException(e)
        null
    }
}

/**
 * Extension функция для валидации и выполнения
 */
suspend fun <T, R> ApplicationCall.validateAndExecute(
    data: T,
    validator: (T) -> T,
    block: suspend (T) -> R
): R? {
    return try {
        val validated = validator(data)
        block(validated)
    } catch (e: Exception) {
        handleException(e)
        null
    }
}

/**
 * Логгер для ошибок
 */
object ErrorLogger {
    private val logger = LoggerFactory.getLogger("ErrorLogger")
    
    fun logValidationError(field: String, message: String) {
        logger.warn("Ошибка валидации - $field: $message")
    }
    
    fun logAuthError(message: String) {
        logger.warn("Ошибка аутентификации: $message")
    }
    
    fun logDatabaseError(operation: String, error: Throwable) {
        logger.error("Ошибка БД при операции $operation", error)
    }
    
    fun logBusinessError(context: String, message: String) {
        logger.warn("Бизнес-ошибка в $context: $message")
    }
    
    fun logInternalError(context: String, error: Throwable) {
        logger.error("Внутренняя ошибка в $context", error)
    }
}

