package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.security.withRole
import io.ktor.server.auth.*
import com.learning.models.ErrorResponse
import com.learning.models.RoadmapRequest
import com.learning.services.RoadmapService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.roadmapRoutes(roadmapService: RoadmapService) {

    route("/roadmap") {
        get {
            try {
                val roadmap = roadmapService.getRoadmap()
                if (roadmap != null) {
                    call.respond(roadmap)
                } else {
                    call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = ErrorResponse(error = "NOT_FOUND", message = "Роадмап не найден")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении roadmap",
                        details = e.message
                    )
                )
            }
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                put {
                    try {
                        val request = call.receive<RoadmapRequest>()
                        val roadmap = roadmapService.updateRoadmap(request)
                        call.respond(roadmap)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при обновлении roadmap",
                                details = e.message
                            )
                        )
                    }
                }
            }
        }
    }
}