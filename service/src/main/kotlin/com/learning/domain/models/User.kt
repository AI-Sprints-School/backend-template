package com.learning.domain.models

import java.time.Instant
import java.util.*

data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val isEmailVerified: Boolean = false,
    val emailVerificationToken: String? = null,
    val passwordResetToken: String? = null,
    val passwordResetExpires: Instant? = null,
    val googleId: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    /** [Roles.STUDENT] или [Roles.ADMIN]; попадает в access-токен claim-ом `role`. */
    val role: String = Roles.STUDENT,
) {
    val isAdmin: Boolean
        get() = role == Roles.ADMIN

    val fullName: String
        get() = "$firstName $lastName"

    val isPasswordResetTokenValid: Boolean
        get() = passwordResetToken != null &&
                passwordResetExpires != null &&
                passwordResetExpires!!.isAfter(Instant.now())
}

/** Роли пользователей. Запись курсов и уроков — только [ADMIN]. */
object Roles {
    const val STUDENT = "student"
    const val ADMIN = "admin"
    val ALL = setOf(STUDENT, ADMIN)
}
