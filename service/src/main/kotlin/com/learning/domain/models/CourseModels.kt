package com.learning.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Serializable
data class CourseRequest(
    val title: String,
    val description: String,
    val coverImage: String? = null,
    val category: String,
    val difficulty: String,
    val duration: Int,
    @Contextual val price: java.math.BigDecimal,
    val isPremium: Boolean = false
)

@Serializable
data class CourseResponse(
    val id: String,
    val title: String,
    val description: String,
    val coverImage: String?,
    val category: String,
    val difficulty: String,
    val duration: Int,
    val lessonsCount: Int,
    @Contextual val rating: java.math.BigDecimal,
    val studentsCount: Int,
    @Contextual val price: java.math.BigDecimal,
    val isPremium: Boolean,
    val createdAt: String,
    val updatedAt: String
)