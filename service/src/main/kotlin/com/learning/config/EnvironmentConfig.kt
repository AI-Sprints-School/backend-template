package com.learning.config

import org.slf4j.LoggerFactory

/**
 * Конфигурация переменных окружения с валидацией при старте приложения
 */
object EnvironmentConfig {
    private val logger = LoggerFactory.getLogger(EnvironmentConfig::class.java)

    // Database
    val databaseUrl: String by lazy {
        getEnv("DATABASE_URL", "jdbc:postgresql://127.0.0.1:5432/learning_platform")
    }
    val databaseUser: String by lazy {
        getEnv("DATABASE_USER", "postgres")
    }
    val databasePassword: String by lazy {
        getEnv("DATABASE_PASSWORD", "postgres_password")
    }
    val databaseMaxPoolSize: Int by lazy {
        getEnv("DATABASE_MAX_POOL_SIZE", "10").toIntOrNull() ?: 10
    }

    // JWT
    /**
     * Секреты-заглушки из репозитория: значение по умолчанию здесь и в application.yaml,
     * и значение из docker-compose.yml и env.example. В production отклоняются оба.
     */
    val DEFAULT_JWT_SECRETS = setOf(
        "your-256-bit-secret-key-change-in-production-please-make-it-long-enough",
        "change_this_secret_key_in_production_to_random_string",
    )

    val jwtSecret: String by lazy {
        getEnv("JWT_SECRET", "your-256-bit-secret-key-change-in-production-please-make-it-long-enough")
    }
    val jwtIssuer: String by lazy {
        getEnv("JWT_ISSUER", "backend-starter-course")
    }
    val jwtAudience: String by lazy {
        getEnv("JWT_AUDIENCE", "backend-starter-course-users")
    }
    val jwtAccessTokenExpiration: Long by lazy {
        getEnv("JWT_ACCESS_TOKEN_EXPIRATION", "3600").toLongOrNull() ?: 3600L
    }
    val jwtRefreshTokenExpiration: Long by lazy {
        getEnv("JWT_REFRESH_TOKEN_EXPIRATION", "604800").toLongOrNull() ?: 604800L
    }

    // Server
    val serverPort: Int by lazy {
        getEnv("SERVER_PORT", "8080").toIntOrNull() ?: 8080
    }
    val serverHost: String by lazy {
        getEnv("SERVER_HOST", "0.0.0.0")
    }

    // CORS
    val allowedOrigins: List<String> by lazy {
        getEnv("ALLOWED_ORIGINS", "*").split(",").map { it.trim() }
    }

    // Email (SMTP)
    val smtpHost: String by lazy {
        getEnv("SMTP_HOST", "smtp.gmail.com")
    }
    val smtpPort: Int by lazy {
        getEnv("SMTP_PORT", "587").toIntOrNull() ?: 587
    }
    val smtpUsername: String by lazy {
        getEnv("SMTP_USERNAME", "")
    }
    val smtpPassword: String by lazy {
        getEnv("SMTP_PASSWORD", "")
    }
    val smtpFrom: String by lazy {
        getEnv("SMTP_FROM", "noreply@learning-platform.com")
    }

    // Redis
    val redisHost: String by lazy {
        getEnv("REDIS_HOST", "localhost")
    }
    val redisPort: Int by lazy {
        getEnv("REDIS_PORT", "6379").toIntOrNull() ?: 6379
    }
    val redisPassword: String? by lazy {
        getEnv("REDIS_PASSWORD", "").takeIf { it.isNotBlank() }
    }

    // Environment
    val environment: String by lazy {
        getEnv("ENVIRONMENT", "development")
    }
    val isProduction: Boolean by lazy {
        environment.equals("production", ignoreCase = true)
    }
    val isDevelopment: Boolean by lazy {
        environment.equals("development", ignoreCase = true)
    }

    // Logging
    val logLevel: String by lazy {
        getEnv("LOG_LEVEL", "INFO")
    }

    /**
     * Валидация всех обязательных переменных при старте
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Проверка критически важных переменных в production
        if (isProduction) {
            // JWT Secret
            if (jwtSecret in DEFAULT_JWT_SECRETS || jwtSecret.length < 32) {
                errors.add("JWT_SECRET должен быть изменен и содержать минимум 32 символа в production")
            }

            // Database
            if (databasePassword == "postgres_password") {
                errors.add("DATABASE_PASSWORD должен быть изменен в production")
            }

            // SMTP (если нужна отправка email)
            if (smtpUsername.isBlank()) {
                warnings.add("SMTP_USERNAME не настроен - отправка email будет недоступна")
            }
        }

        // Общие проверки
        if (databaseUrl.isBlank()) {
            errors.add("DATABASE_URL не может быть пустым")
        }

        if (jwtAccessTokenExpiration <= 0) {
            errors.add("JWT_ACCESS_TOKEN_EXPIRATION должен быть положительным числом")
        }

        if (serverPort !in 1..65535) {
            errors.add("SERVER_PORT должен быть в диапазоне 1-65535")
        }

        return ValidationResult(errors, warnings)
    }

    /**
     * Логирование текущей конфигурации (без секретов)
     */
    fun logConfiguration() {
        logger.info("=== Environment Configuration ===")
        logger.info("Environment: $environment")
        logger.info("Server: $serverHost:$serverPort")
        logger.info("Database URL: ${maskSensitive(databaseUrl)}")
        logger.info("Database User: $databaseUser")
        logger.info("Database Max Pool Size: $databaseMaxPoolSize")
        logger.info("JWT Issuer: $jwtIssuer")
        logger.info("JWT Access Token Expiration: ${jwtAccessTokenExpiration}s")
        logger.info("JWT Refresh Token Expiration: ${jwtRefreshTokenExpiration}s")
        logger.info("CORS Allowed Origins: ${allowedOrigins.joinToString(", ")}")
        logger.info("SMTP Host: $smtpHost:$smtpPort")
        logger.info("Redis: $redisHost:$redisPort")
        logger.info("Log Level: $logLevel")
        logger.info("================================")
    }

    /**
     * Инициализация и валидация конфигурации
     * @throws IllegalStateException если есть критические ошибки
     */
    fun init() {
        logConfiguration()

        val result = validate()

        result.warnings.forEach { warning ->
            logger.warn("Configuration warning: $warning")
        }

        if (result.hasErrors) {
            result.errors.forEach { error ->
                logger.error("Configuration error: $error")
            }
            throw IllegalStateException(
                "Configuration validation failed with ${result.errors.size} error(s):\n" +
                        result.errors.joinToString("\n") { "  - $it" }
            )
        }

        logger.info("Configuration validation passed")
    }

    private fun getEnv(name: String, default: String): String {
        return System.getenv(name) ?: default
    }

    private fun maskSensitive(value: String): String {
        // Маскируем пароли в URL
        return value.replace(Regex("://([^:]+):([^@]+)@"), "://$1:****@")
    }

    data class ValidationResult(
        val errors: List<String>,
        val warnings: List<String>
    ) {
        val hasErrors: Boolean get() = errors.isNotEmpty()
        val hasWarnings: Boolean get() = warnings.isNotEmpty()
        val isValid: Boolean get() = !hasErrors
    }
}
