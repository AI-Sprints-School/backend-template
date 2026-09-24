package com.learning.domain.models

import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class Course(
    val id: UUID,
    val title: String,
    val description: String,
    val shortDescription: String? = null,
    val thumbnailUrl: String? = null,
    val price: BigDecimal,
    val originalPrice: BigDecimal? = null,
    val duration: Int, // in minutes
    val difficulty: String, // beginner, intermediate, advanced
    val isPublished: Boolean = false,
    val lessonsCount: Int = 0,
    val authorId: UUID? = null, // автор курса; черновик видят он и admin
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val hasDiscount: Boolean
        get() = originalPrice != null && originalPrice > price
    
    val discountPercentage: Int?
        get() = if (hasDiscount) {
            val discount = originalPrice!!.subtract(price)
            discount.divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toInt()
        } else null
    
    val formattedPrice: String
        get() = "$${price.setScale(2, java.math.RoundingMode.HALF_UP)}"
    
    val formattedOriginalPrice: String?
        get() = originalPrice?.let { "$${it.setScale(2, java.math.RoundingMode.HALF_UP)}" }
    
    val formattedDuration: String
        get() = when {
            duration < 60 -> "${duration} мин"
            duration < 1440 -> "${duration / 60} ч ${duration % 60} мин"
            else -> "${duration / 1440} дн ${(duration % 1440) / 60} ч"
        }
}
