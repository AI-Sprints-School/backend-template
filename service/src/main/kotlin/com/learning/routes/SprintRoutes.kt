package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.security.withRole
import io.ktor.server.auth.*
import com.learning.models.ErrorResponse
import com.learning.models.SprintRequest
import com.learning.models.SprintsResponse
import com.learning.services.SprintService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sprintRoutes(sprintService: SprintService) {

    route("/sprints") {
        get {
            try {
                val sprints = sprintService.getAllSprints()
                call.respond(SprintsResponse(sprints = sprints))
            } catch (e: Exception) {
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении спринтов",
                        details = e.message
                    )
                )
            }
        }

        get("/{id}") {
            try {
                val sprintId = call.parameters["id"] ?: throw IllegalArgumentException("ID спринта не указан")
                val sprint = sprintService.getSprintById(sprintId)

                if (sprint != null) {
                    call.respond(sprint)
                } else {
                    call.respond(
                        status = io.ktor.http.HttpStatusCode.NotFound,
                        message = ErrorResponse(
                            error = "SPRINT_NOT_FOUND",
                            message = "Спринт не найден"
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = io.ktor.http.HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "INVALID_REQUEST",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = io.ktor.http.HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении спринта",
                        details = e.message
                    )
                )
            }
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                post {
                    try {
                        val request = call.receive<SprintRequest>()
                        val sprint = sprintService.createSprint(request)
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.Created,
                            message = sprint
                        )
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
                                message = "Ошибка при создании спринта",
                                details = e.message
                            )
                        )
                    }
                }

                put("/{id}") {
                    try {
                        val sprintId = call.parameters["id"] ?: throw IllegalArgumentException("ID спринта не указан")
                        val request = call.receive<SprintRequest>()
                        val sprint = sprintService.updateSprint(sprintId, request)
                        call.respond(sprint)
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
                                message = "Ошибка при обновлении спринта",
                                details = e.message
                            )
                        )
                    }
                }

                delete("/{id}") {
                    try {
                        val sprintId = call.parameters["id"] ?: throw IllegalArgumentException("ID спринта не указан")
                        val deleted = sprintService.deleteSprint(sprintId)
                        if (deleted) {
                            call.respond(
                                status = io.ktor.http.HttpStatusCode.NoContent,
                                message = mapOf("message" to "Спринт успешно удален")
                            )
                        } else {
                            call.respond(
                                status = io.ktor.http.HttpStatusCode.NotFound,
                                message = ErrorResponse(
                                    error = "SPRINT_NOT_FOUND",
                                    message = "Спринт не найден"
                                )
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "INVALID_REQUEST",
                                message = e.message ?: "Неверный запрос"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = io.ktor.http.HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при удалении спринта",
                                details = e.message
                            )
                        )
                    }
                }
            }
        }
    }
}