package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.models.LessonContentsResponse
import com.learning.models.LessonRequest
import com.learning.models.LessonsResponse
import com.learning.security.withRole
import com.learning.services.CourseService
import com.learning.services.LessonService
import com.learning.utils.AppException
import com.learning.utils.safeExecute
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Уроки. Чтение — всем, запись — только администратору.
 * Отметка «урок пройден» — `POST /lessons/{id}/complete`, см. [userRoutes].
 */
fun Route.lessonRoutes(lessonService: LessonService, courseService: CourseService) {

    route("/lessons") {
        get {
            TODO("Глава 4, урок 17: GET /lessons")
        }

        get("/{id}") {
            TODO("Глава 4, урок 17: GET /lessons/{id}")
        }

        get("/{id}/content") {
            TODO("Глава 4, урок 17: GET /lessons/{id}/content")
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                post {
                    TODO("Глава 4, урок 17: POST /lessons — 201")
                }

                put("/{id}") {
                    TODO("Глава 4, урок 17: PUT /lessons/{id}")
                }

                delete("/{id}") {
                    TODO("Глава 4, урок 17: DELETE /lessons/{id} — 204")
                }
            }
        }
    }
}

internal fun io.ktor.server.application.ApplicationCall.lessonId(): String =
    uuidParameter("id", "Неверный ID урока")
