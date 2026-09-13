package com.learning.domain.models

import java.time.Instant
import java.util.*

data class Sprint(
    val id: UUID,
    val title: String,
    val description: String,
    val discountPercentage: Int,
    val startDate: Instant,
    val endDate: Instant,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isCurrentlyActive: Boolean
        get() = isActive && startDate.isBefore(Instant.now()) && endDate.isAfter(Instant.now())

    val isExpired: Boolean
        get() = endDate.isBefore(Instant.now())

    val isUpcoming: Boolean
        get() = startDate.isAfter(Instant.now())

    val formattedDiscount: String
        get() = "$discountPercentage%"

    val formattedDateRange: String
        get() = "${startDate.toString().substring(0, 10)} - ${endDate.toString().substring(0, 10)}"
}
