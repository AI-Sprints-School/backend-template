package com.learning.routes

import com.learning.domain.models.Roles
import com.learning.security.withRole
import io.ktor.server.auth.*
import com.learning.models.ErrorResponse
import com.learning.models.InterviewAnswerRequest
import com.learning.models.InterviewQuestionRequest
import com.learning.models.InterviewQuestionResponse
import com.learning.services.InterviewService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.interviewRoutes(interviewService: InterviewService) {

    route("/interview") {
        get("/questions") {
            try {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val category = call.request.queryParameters["category"]

                val response = interviewService.getInterviewQuestions(page, limit, category)
                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении вопросов собеседования",
                        details = e.message
                    )
                )
            }
        }

        get("/questions/{id}") {
            try {
                val questionId = call.parameters["id"] ?: throw IllegalArgumentException("ID вопроса не указан")
                val question = interviewService.getInterviewQuestionById(questionId)

                if (question != null) {
                    call.respond(question)
                } else {
                    call.respond(
                        status = HttpStatusCode.NotFound,
                        message = ErrorResponse(
                            error = "QUESTION_NOT_FOUND",
                            message = "Вопрос не найден"
                        )
                    )
                }
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "INVALID_REQUEST",
                        message = e.message ?: "Неверный запрос"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при получении вопроса",
                        details = e.message
                    )
                )
            }
        }

        post("/submit") {
            try {
                val request = call.receive<InterviewAnswerRequest>()
                val response = interviewService.submitInterviewAnswer(request)
                call.respond(response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        error = "VALIDATION_ERROR",
                        message = e.message ?: "Ошибка валидации данных"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorResponse(
                        error = "INTERNAL_ERROR",
                        message = "Ошибка при отправке ответа",
                        details = e.message
                    )
                )
            }
        }

        // Запись — только администратор: без токена 401, без роли 403
        authenticate("auth-jwt") {
            withRole(Roles.ADMIN) {
                post("/questions") {
                    try {
                        val request = call.receive<InterviewQuestionRequest>()
                        val question: InterviewQuestionResponse = interviewService.createInterviewQuestion(request)
                        call.respond(
                            status = HttpStatusCode.Created,
                            message = question
                        )
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при создании вопроса",
                                details = e.message
                            )
                        )
                    }
                }

                put("/questions/{id}") {
                    try {
                        val questionId = call.parameters["id"] ?: throw IllegalArgumentException("ID вопроса не указан")
                        val request = call.receive<InterviewQuestionRequest>()
                        val question = interviewService.updateInterviewQuestion(questionId, request)
                        call.respond(question)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "VALIDATION_ERROR",
                                message = e.message ?: "Ошибка валидации данных"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при обновлении вопроса",
                                details = e.message
                            )
                        )
                    }
                }

                delete("/questions/{id}") {
                    try {
                        val questionId = call.parameters["id"] ?: throw IllegalArgumentException("ID вопроса не указан")
                        val deleted = interviewService.deleteInterviewQuestion(questionId)
                        if (deleted) {
                            call.respond(
                                status = HttpStatusCode.NoContent,
                                message = mapOf("message" to "Вопрос успешно удален")
                            )
                        } else {
                            call.respond(
                                status = HttpStatusCode.NotFound,
                                message = ErrorResponse(
                                    error = "QUESTION_NOT_FOUND",
                                    message = "Вопрос не найден"
                                )
                            )
                        }
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            status = HttpStatusCode.BadRequest,
                            message = ErrorResponse(
                                error = "INVALID_REQUEST",
                                message = e.message ?: "Неверный запрос"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            status = HttpStatusCode.InternalServerError,
                            message = ErrorResponse(
                                error = "INTERNAL_ERROR",
                                message = "Ошибка при удалении вопроса",
                                details = e.message
                            )
                        )
                    }
                }
            }
        }
    }
}