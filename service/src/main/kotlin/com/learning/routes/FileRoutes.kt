package com.learning.routes

import com.learning.models.MessageResponse
import com.learning.services.FileStorageService
import com.learning.utils.AppException
import com.learning.utils.safeExecute
import io.ktor.utils.io.toByteArray
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResponse(
    val fileName: String,
    val originalFileName: String,
    val url: String,
    val size: Long,
    val mimeType: String
)

@Serializable
data class FileInfoResponse(
    val fileName: String,
    val size: Long,
    val mimeType: String,
    val lastModified: Long
)

fun Route.fileRoutes(fileStorageService: FileStorageService) {

    route("/files") {

        /**
         * POST /api/v1/files/upload
         * Загрузка файла (требует авторизации)
         */
        authenticate("auth-jwt") {
            post("/upload") {
                call.safeExecute {
                    val multipart = call.receiveMultipart()
                    var fileUploadResult: FileStorageService.FileUploadResult? = null
                    var subDirectory = ""

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "directory") {
                                    subDirectory = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName ?: "unknown"
                                val contentType = part.contentType?.toString()
                                val bytes = part.provider().toByteArray()

                                fileUploadResult = fileStorageService.saveFile(
                                    bytes = bytes,
                                    originalFileName = fileName,
                                    contentType = contentType,
                                    subDirectory = subDirectory
                                )
                            }
                            else -> {}
                        }
                        part.release()
                    }

                    when (val result = fileUploadResult) {
                        is FileStorageService.FileUploadResult.Success -> {
                            call.respond(
                                HttpStatusCode.Created,
                                FileUploadResponse(
                                    fileName = result.fileName,
                                    originalFileName = result.originalFileName,
                                    url = result.url,
                                    size = result.size,
                                    mimeType = result.mimeType
                                )
                            )
                        }
                        is FileStorageService.FileUploadResult.Error -> {
                            throw AppException.BadRequestError(result.message)
                        }
                        null -> {
                            throw AppException.BadRequestError("Файл не найден в запросе")
                        }
                    }
                }
            }

            /**
             * POST /api/v1/files/upload/avatar
             * Загрузка аватара пользователя
             */
            post("/upload/avatar") {
                call.safeExecute {
                    val multipart = call.receiveMultipart()
                    var fileUploadResult: FileStorageService.FileUploadResult? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                val fileName = part.originalFileName ?: "avatar"
                                val contentType = part.contentType?.toString()
                                val bytes = part.provider().toByteArray()

                                // Ограничение на размер аватара - 2MB
                                if (bytes.size > 2 * 1024 * 1024) {
                                    throw AppException.BadRequestError("Размер аватара не должен превышать 2MB")
                                }

                                // Только изображения
                                val allowedTypes = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
                                if (contentType != null && contentType !in allowedTypes) {
                                    throw AppException.BadRequestError("Разрешены только изображения (JPEG, PNG, GIF, WebP)")
                                }

                                fileUploadResult = fileStorageService.saveFile(
                                    bytes = bytes,
                                    originalFileName = fileName,
                                    contentType = contentType,
                                    subDirectory = "avatars"
                                )
                            }
                            else -> {}
                        }
                        part.release()
                    }

                    when (val result = fileUploadResult) {
                        is FileStorageService.FileUploadResult.Success -> {
                            call.respond(
                                HttpStatusCode.Created,
                                FileUploadResponse(
                                    fileName = result.fileName,
                                    originalFileName = result.originalFileName,
                                    url = result.url,
                                    size = result.size,
                                    mimeType = result.mimeType
                                )
                            )
                        }
                        is FileStorageService.FileUploadResult.Error -> {
                            throw AppException.BadRequestError(result.message)
                        }
                        null -> {
                            throw AppException.BadRequestError("Файл не найден в запросе")
                        }
                    }
                }
            }

            /**
             * DELETE /api/v1/files/{fileName}
             * Удаление файла
             */
            delete("/{fileName}") {
                call.safeExecute {
                    val fileName = call.parameters["fileName"]
                        ?: throw AppException.BadRequestError("Имя файла обязательно")
                    val subDirectory = call.request.queryParameters["directory"] ?: ""

                    val deleted = fileStorageService.deleteFile(fileName, subDirectory)

                    if (deleted) {
                        call.respond(HttpStatusCode.OK, MessageResponse("Файл удален"))
                    } else {
                        throw AppException.NotFoundError("Файл не найден")
                    }
                }
            }
        }

        /**
         * GET /api/v1/files/{fileName}
         * Получение файла (публичный доступ)
         */
        get("/{fileName}") {
            val fileName = call.parameters["fileName"]
                ?: throw AppException.BadRequestError("Имя файла обязательно")
            val subDirectory = call.request.queryParameters["directory"] ?: ""

            val fileInfo = fileStorageService.getFileInfo(fileName, subDirectory)
            if (fileInfo == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Файл не найден"))
                return@get
            }

            val fileBytes = fileStorageService.getFile(fileName, subDirectory)
            if (fileBytes == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Файл не найден"))
                return@get
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                "inline; filename=\"$fileName\""
            )
            call.respondBytes(fileBytes, ContentType.parse(fileInfo.mimeType))
        }

        /**
         * GET /api/v1/files/{subDirectory}/{fileName}
         * Получение файла из поддиректории
         */
        get("/{subDirectory}/{fileName}") {
            val subDirectory = call.parameters["subDirectory"] ?: ""
            val fileName = call.parameters["fileName"]
                ?: throw AppException.BadRequestError("Имя файла обязательно")

            val fileInfo = fileStorageService.getFileInfo(fileName, subDirectory)
            if (fileInfo == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Файл не найден"))
                return@get
            }

            val fileBytes = fileStorageService.getFile(fileName, subDirectory)
            if (fileBytes == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Файл не найден"))
                return@get
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                "inline; filename=\"$fileName\""
            )
            call.respondBytes(fileBytes, ContentType.parse(fileInfo.mimeType))
        }

        /**
         * GET /api/v1/files/{fileName}/info
         * Получение информации о файле
         */
        get("/{fileName}/info") {
            val fileName = call.parameters["fileName"]
                ?: throw AppException.BadRequestError("Имя файла обязательно")
            val subDirectory = call.request.queryParameters["directory"] ?: ""

            val fileInfo = fileStorageService.getFileInfo(fileName, subDirectory)
                ?: throw AppException.NotFoundError("Файл не найден")

            call.respond(
                HttpStatusCode.OK,
                FileInfoResponse(
                    fileName = fileInfo.fileName,
                    size = fileInfo.size,
                    mimeType = fileInfo.mimeType,
                    lastModified = fileInfo.lastModified
                )
            )
        }
    }
}
