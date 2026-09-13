package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.models.*
import com.learning.security.withRole
import com.learning.services.CourseService
import com.learning.utils.AppException
import com.learning.utils.safeExecute
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Курсы. Чтение — всем, запись — только администратору.
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

        get("/{id}") {
            TODO("Глава 4, урок 16: GET /courses/{id}")
        }

        get("/{id}/lessons") {
            TODO("Глава 4, урок 16: GET /courses/{id}/lessons")
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

/** ID курса из пути; неверный UUID — 400, а не 500. */
internal fun io.ktor.server.application.ApplicationCall.courseId(): String =
    uuidParameter("id", "Неверный ID курса")

internal fun io.ktor.server.application.ApplicationCall.uuidParameter(name: String, message: String): String {
    TODO("Глава 2, урок 8: UUID из пути, неверный — 400")
}
