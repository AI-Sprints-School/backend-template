package com.learning.routes

import com.learning.models.*
import com.learning.services.UserService
import com.learning.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Профиль и прогресс текущего пользователя. Пользователь всегда берётся из
 * токена (`sub`), а не из запроса: чужой профиль и чужой прогресс через эти
 * маршруты недоступны.
 */
fun Route.userRoutes(userService: UserService) {

    authenticate("auth-jwt") {
        route("/user") {
            /** GET /api/v1/user/profile */
            get("/profile") {
                TODO("Глава 5, урок 22: GET /user/profile")
            }

            /** PUT /api/v1/user/profile */
            put("/profile") {
                TODO("Глава 5, урок 22: PUT /user/profile")
            }

            /** GET /api/v1/user/progress */
            get("/progress") {
                TODO("Глава 5, урок 24: GET /user/progress")
            }
        }

        /** POST /api/v1/lessons/{id}/complete — отметить урок пройденным (спецификация, 3.3.2) */
        post("/lessons/{id}/complete") {
            TODO("Глава 5, урок 24: POST /lessons/{id}/complete")
        }
    }
}

/** ID пользователя из проверенного токена. */
fun ApplicationCall.currentUserId(): String =
    principal<JWTPrincipal>()?.payload?.subject
        ?: throw AppException.AuthenticationError("Пользователь не авторизован")
