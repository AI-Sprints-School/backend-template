package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.domain.models.Viewer
import com.learning.security.JwtService
import com.learning.models.*
import com.learning.security.withRole
import com.learning.services.CourseService
import com.learning.utils.AppException
import com.learning.utils.safeExecute
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Курсы. Чтение — всем, запись — только администратору.
 * Черновик по id и его уроки видят только автор и администратор, остальным — 404, как несуществующий курс.
 *
 * Коды ответов: неверный UUID → 400, нет курса → 404, нет токена → 401,
 * нет роли admin → 403, ошибка валидации тела → 400 `VALIDATION_ERROR`.
 */
fun Route.courseRoutes(courseService: CourseService) {

    route("/courses") {
        // Каталог: только опубликованные курсы
        get {
            TODO("Глава 4, урок 16: GET /courses — опубликованные курсы")
        }

        // Токен необязателен: без него — аноним, с ним сервис узнаёт автора и администратора
        authenticate("auth-jwt", optional = true) {
            get("/{id}") {
                TODO("Глава 4, урок 16: GET /courses/{id}")
            }

            // Уроки курса подчиняются тому же правилу: курс, которого зрителю не видно, — 404
            get("/{id}/lessons") {
                TODO("Глава 4, урок 16: GET /courses/{id}/lessons")
            }
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                post {
                    TODO("Глава 4, урок 17: POST /courses — 201")
                }

                put("/{id}") {
                    TODO("Глава 4, урок 17: PUT /courses/{id}")
                }

                delete("/{id}") {
                    TODO("Глава 4, урок 17: DELETE /courses/{id} — 204")
                }
            }
        }
    }
}

/**
 * Кто спрашивает: пользователь и роль из проверенного токена, без токена — `null`.
 * Работает под `authenticate` — в том числе с `optional = true`.
 */
internal fun io.ktor.server.application.ApplicationCall.viewer(): Viewer? {
    val payload = principal<JWTPrincipal>()?.payload ?: return null
    val userId = payload.subject?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() } ?: return null
    val role = payload.getClaim(JwtService.CLAIM_ROLE)?.asString() ?: return null
    return Viewer(userId, role)
}

/** ID курса из пути; неверный UUID — 400, а не 500. */
internal fun io.ktor.server.application.ApplicationCall.courseId(): String =
    uuidParameter("id", "Неверный ID курса")

internal fun io.ktor.server.application.ApplicationCall.uuidParameter(name: String, message: String): String {
    TODO("Глава 2, урок 8: UUID из пути, неверный — 400")
}
