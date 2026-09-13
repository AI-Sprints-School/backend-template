package com.learning

import com.learning.config.EnvironmentConfig
import com.learning.config.RedisConfig
import com.learning.database.DatabaseConfig
import com.learning.database.DatabaseSettings
import com.learning.jobs.JobScheduler
import com.learning.models.*
import com.learning.repositories.*
import com.learning.routes.*
import com.learning.security.AuthService
import com.learning.security.JwtService
import com.learning.security.JwtSettings
import com.learning.services.*
import com.learning.utils.ErrorTypes
import com.learning.validation.registerValidators
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.di.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Точка входа модуля. Порядок: конфигурация → база → зависимости → плагины → маршруты.
 */
suspend fun Application.module() {
    val config = environment.config

    // ---------- Конфигурация ----------
    try {
        EnvironmentConfig.init()
    } catch (e: Exception) {
        log.error("Ошибка конфигурации: {}", e.message)
        if (EnvironmentConfig.isProduction) throw e
    }

    // ---------- База данных: пул, миграции Flyway, Exposed ----------
    DatabaseConfig.init(config.databaseSettings())

    // ---------- Redis (опционально) ----------
    if (config.boolean("redis.enabled", default = true)) {
        if (RedisConfig.init()) log.info("Redis подключен") else log.warn("Redis недоступен, кеширование отключено")
    }

    // ---------- Зависимости (встроенный DI Ktor) ----------
    dependencies {
        provide { config.jwtSettings() }
        provide(::JwtService)

        provide(::UserRepository)
        provide(::CourseRepository)
        provide(::LessonRepository)
        provide(::QuizRepository)
        provide(::QuizQuestionRepository)
        provide(::TokenRepository)
        provide(::SprintRepository)
        provide(::InterviewRepository)
        provide(::RoadmapRepository)
        provide(::ProgressRepository)

        provide { AuthService(resolve(), resolve(), resolve()) }
        provide(::CourseService)
        provide<LessonService> { LessonService(resolve()) }
        provide(::TestService)
        provide(::SprintService)
        provide(::InterviewService)
        provide(::RoadmapService)
        provide { UserService(resolve(), resolve(), resolve()) }
        provide { EmailService() }
        provide { FileStorageService() }
        provide(::JobScheduler) cleanup { it.stop() }
    }

    val jwtService: JwtService = dependencies.resolve()

    // ---------- Первый администратор ----------
    // Роль admin выдаётся существующему пользователю по почте из ADMIN_EMAIL.
    // Пароль в репозиторий и в миграции не кладём.
    config.propertyOrNull("admin.email")?.getString()?.takeIf { it.isNotBlank() }?.let { email ->
        val users: UserRepository = dependencies.resolve()
        val user = users.findByEmail(email)
        if (user == null) {
            log.warn("ADMIN_EMAIL={} задан, но пользователь не зарегистрирован", email)
        } else if (!user.isAdmin && users.updateRole(user.id, com.learning.domain.models.Roles.ADMIN)) {
            log.info("Пользователю {} выдана роль admin", email)
        }
    }

    // ---------- Фоновые задачи ----------
    if (config.boolean("jobs.enabled", default = true)) {
        dependencies.resolve<JobScheduler>().start()
    }

    monitor.subscribe(ApplicationStopped) {
        RedisConfig.close()
        DatabaseConfig.close()
    }

    // ---------- Плагины ----------
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
        format { call -> "${call.request.httpMethod.value} ${call.request.path()} → ${call.response.status()}" }
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(RequestValidation) {
        registerValidators()
    }

    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(ErrorTypes.VALIDATION_ERROR, "Ошибка валидации данных", cause.reasons.joinToString("; "))
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorTypes.BAD_REQUEST, cause.message ?: "Некорректный запрос"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Необработанная ошибка", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(ErrorTypes.INTERNAL_ERROR, "Внутренняя ошибка сервера"))
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(RateLimit) {
        register(RateLimitName("auth")) {
            rateLimiter(limit = config.int("rateLimit.auth", 5), refillPeriod = 1.minutes)
        }
        register(RateLimitName("api")) {
            rateLimiter(limit = config.int("rateLimit.api", 100), refillPeriod = 1.minutes)
        }
    }

    install(SecurityHeaders)

    // Один механизм JWT: плагин проверяет токен тем же верификатором, что выпускает JwtService
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtService.realm
            verifier(jwtService.verifier)
            validate { credential ->
                val isAccessToken = credential.payload.getClaim(JwtService.CLAIM_TYPE).asString() == JwtService.TYPE_ACCESS
                if (credential.payload.subject != null && isAccessToken) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "UNAUTHORIZED", "message" to "Токен недействителен или отсутствует")
                )
            }
        }
    }

    // ---------- Маршруты: зависимости приходят параметрами, маршрут ничего не создаёт сам ----------
    val authService: AuthService = dependencies.resolve()
    val tokenRepository: TokenRepository = dependencies.resolve()
    val userRepository: UserRepository = dependencies.resolve()
    val emailService: EmailService = dependencies.resolve()
    val courseService: CourseService = dependencies.resolve()
    val lessonService: LessonService = dependencies.resolve()
    val testService: TestService = dependencies.resolve()
    val sprintService: SprintService = dependencies.resolve()
    val interviewService: InterviewService = dependencies.resolve()
    val roadmapService: RoadmapService = dependencies.resolve()
    val fileStorageService: FileStorageService = dependencies.resolve()
    val userService: UserService = dependencies.resolve()

    routing {
        healthRoutes()

        route("/api/v1") {
            rateLimit(RateLimitName("auth")) {
                authRoutes(authService, tokenRepository, userRepository, emailService)
            }

            rateLimit(RateLimitName("api")) {
                courseRoutes(courseService)
                lessonRoutes(lessonService, courseService)
                testRoutes(testService)
                sprintRoutes(sprintService)
                interviewRoutes(interviewService)
                roadmapRoutes(roadmapService)
                fileRoutes(fileStorageService)
            }

            rateLimit(RateLimitName("api")) {
                userRoutes(userService)
            }
        }
    }
}

/** Заголовки безопасности на каждый ответ. */
private val SecurityHeaders = createApplicationPlugin("SecurityHeaders") {
    onCall { call ->
        call.response.headers.apply {
            append("X-Content-Type-Options", "nosniff")
            append("X-Frame-Options", "DENY")
            append("Referrer-Policy", "strict-origin-when-cross-origin")
            if (EnvironmentConfig.isProduction) {
                append("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
            }
        }
    }
}

private fun Route.healthRoutes() {
    // Полная проверка: база, пул, память
    get("/health") {
        TODO("Глава 1, урок 3: полная проверка — база, пул, память")
    }

    // Liveness: процесс жив
    get("/health/live") {
        TODO("Глава 1, урок 3: процесс жив")
    }

    // Readiness: готов принимать трафик, база отвечает
    get("/health/ready") {
        TODO("Глава 1, урок 3: база отвечает")
    }
}

// ---------- Чтение конфигурации ----------

private fun ApplicationConfig.string(path: String, default: String) = propertyOrNull(path)?.getString() ?: default
private fun ApplicationConfig.int(path: String, default: Int) = propertyOrNull(path)?.getString()?.toIntOrNull() ?: default
private fun ApplicationConfig.long(path: String, default: Long) = propertyOrNull(path)?.getString()?.toLongOrNull() ?: default
private fun ApplicationConfig.boolean(path: String, default: Boolean) = propertyOrNull(path)?.getString()?.toBooleanStrictOrNull() ?: default

private fun ApplicationConfig.databaseSettings() = DatabaseSettings(
    url = string("database.url", "jdbc:postgresql://localhost:5432/learning_platform"),
    user = string("database.user", "postgres"),
    password = string("database.password", "postgres_password"),
    maxPoolSize = int("database.maxPoolSize", 10),
    seed = boolean("database.seed", false),
)

private fun ApplicationConfig.jwtSettings(): JwtSettings {
    val env = JwtSettings.fromEnvironment()
    return JwtSettings(
        secret = string("jwt.secret", env.secret),
        issuer = string("jwt.issuer", env.issuer),
        audience = string("jwt.audience", env.audience),
        realm = string("jwt.realm", env.realm),
        accessTokenTtlSeconds = long("jwt.accessTokenExpiration", env.accessTokenTtlSeconds),
        refreshTokenTtlSeconds = long("jwt.refreshTokenExpiration", env.refreshTokenTtlSeconds),
    )
}
