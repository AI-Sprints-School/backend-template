package com.learning.domain.models

import java.time.Instant
import java.util.*

data class Lesson(
    val id: UUID,
    val courseId: UUID,
    val title: String,
    val description: String? = null,
    val content: String,
    val videoUrl: String? = null,
    val duration: Int, // in minutes
    val order: Int,
    val isPublished: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val formattedDuration: String
        get() = when {
            duration < 60 -> "${duration} мин"
            duration < 1440 -> "${duration / 60} ч ${duration % 60} мин"
            else -> "${duration / 1440} дн ${(duration % 1440) / 60} ч"
        }
    
    val hasVideo: Boolean
        get() = !videoUrl.isNullOrBlank()
}
