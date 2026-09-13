package com.learning.domain.models

import java.time.Instant
import java.util.*

data class InterviewQuestion(
    val id: UUID,
    val question: String,
    val answer: String,
    val category: String,
    val difficulty: String, // easy, medium, hard
    val tags: List<String> = emptyList(),
    val isPublished: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val formattedTags: String
        get() = tags.joinToString(", ")
    
    val difficultyLevel: Int
        get() = when (difficulty.lowercase()) {
            "easy" -> 1
            "medium" -> 2
            "hard" -> 3
            else -> 0
        }
    
    val isHighDifficulty: Boolean
        get() = difficultyLevel >= 3
}
