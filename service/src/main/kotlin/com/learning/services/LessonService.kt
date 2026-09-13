package com.learning.services

import com.learning.models.LessonContentResponse
import com.learning.models.LessonRequest
import com.learning.models.LessonResponse
import com.learning.repositories.LessonRepository
import java.util.*

class LessonService(
    private val lessonRepository: LessonRepository = LessonRepository()
) {

    fun getAllLessons(): List<LessonResponse> {
        TODO("Глава 4, урок 17: LessonService.getAllLessons")
    }

    fun getLessonById(id: String): LessonResponse? {
        TODO("Глава 4, урок 17: LessonService.getLessonById")
    }

    fun getCourseLessons(courseId: String): List<LessonResponse> {
        TODO("Глава 4, урок 17: LessonService.getCourseLessons")
    }

    fun createLesson(request: LessonRequest): LessonResponse {
        TODO("Глава 4, урок 17: LessonService.createLesson")
    }

    fun updateLesson(id: String, request: LessonRequest): LessonResponse {
        TODO("Глава 4, урок 17: LessonService.updateLesson")
    }

    fun deleteLesson(id: String): Boolean {
        TODO("Глава 4, урок 17: LessonService.deleteLesson")
    }

    /** Содержимое урока — один текстовый блок из `lessons.content`; пустой урок — пустой список. */
    fun getLessonContent(lessonId: String): List<LessonContentResponse> {
        TODO("Глава 4, урок 17: LessonService.getLessonContent")
    }
    
    // Extension function for mapping domain model to API response
    private fun com.learning.domain.models.Lesson.toLessonResponse(): LessonResponse {
        return LessonResponse(
            id = this.id.toString(),
            courseId = this.courseId.toString(),
            title = this.title,
            description = this.description ?: "",
            orderIndex = this.order,
            duration = this.duration,
            videoUrl = this.videoUrl,
            isPreview = false, // Can be determined by business logic
            contents = emptyList() // Can be populated from lesson contents
        )
    }
}