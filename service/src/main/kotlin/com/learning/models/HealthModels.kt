package com.learning.models

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val version: String = "1.0.0",
    val checks: HealthChecks
)

@Serializable
data class HealthChecks(
    val database: DatabaseHealth,
    val memory: MemoryHealth
)

@Serializable
data class DatabaseHealth(
    val status: String,
    val responseTimeMs: Long,
    val pool: PoolHealth? = null,
    val error: String? = null
)

@Serializable
data class PoolHealth(
    val active: Int,
    val idle: Int,
    val total: Int,
    val waiting: Int
)

@Serializable
data class MemoryHealth(
    val status: String,
    val usedMb: Long,
    val freeMb: Long,
    val maxMb: Long,
    val usagePercent: Int
)
