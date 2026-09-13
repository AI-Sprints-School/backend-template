package com.learning.domain.models

import java.time.Instant
import java.util.*

data class Roadmap(
    val id: UUID,
    val title: String,
    val description: String,
    val category: String,
    val isPublished: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val formattedCategory: String
        get() = category.replaceFirstChar { it.uppercase() }
}

data class RoadmapStep(
    val id: UUID,
    val roadmapId: UUID,
    val title: String,
    val description: String,
    val order: Int,
    val courseId: UUID? = null,
    val isCompleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val hasCourse: Boolean
        get() = courseId != null
}
