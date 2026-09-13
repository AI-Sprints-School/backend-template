package com.learning.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.slf4j.LoggerFactory

/**
 * Конфигурация Redis клиента
 */
object RedisConfig {
    private val logger = LoggerFactory.getLogger(RedisConfig::class.java)

    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    val host: String get() = EnvironmentConfig.redisHost
    val port: Int get() = EnvironmentConfig.redisPort
    val password: String? get() = EnvironmentConfig.redisPassword

    val isConfigured: Boolean
        get() = host.isNotBlank()

    /**
     * Инициализация Redis клиента
     */
    fun init(): Boolean {
        if (!isConfigured) {
            logger.warn("Redis не настроен: REDIS_HOST не указан")
            return false
        }

        return try {
            val uriBuilder = RedisURI.Builder.redis(host, port)
            password?.let { uriBuilder.withPassword(it.toCharArray()) }

            client = RedisClient.create(uriBuilder.build())
            connection = client?.connect()

            // Тест подключения
            val commands = connection?.sync()
            commands?.ping()

            logger.info("Redis подключен: $host:$port")
            true
        } catch (e: Exception) {
            logger.error("Ошибка подключения к Redis: ${e.message}", e)
            false
        }
    }

    /**
     * Получение синхронных команд Redis
     */
    fun getCommands(): RedisCommands<String, String>? {
        if (connection == null || !connection!!.isOpen) {
            init()
        }
        return connection?.sync()
    }

    /**
     * Проверка подключения
     */
    fun isConnected(): Boolean {
        return try {
            connection?.sync()?.ping() == "PONG"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Закрытие подключения
     */
    fun close() {
        try {
            connection?.close()
            client?.shutdown()
            logger.info("Redis подключение закрыто")
        } catch (e: Exception) {
            logger.error("Ошибка при закрытии Redis: ${e.message}", e)
        }
    }

    /**
     * Проверка здоровья Redis
     */
    fun checkHealth(): RedisHealthStatus {
        return try {
            val startTime = System.currentTimeMillis()
            val pong = connection?.sync()?.ping()
            val responseTime = System.currentTimeMillis() - startTime

            if (pong == "PONG") {
                RedisHealthStatus(
                    status = "UP",
                    responseTimeMs = responseTime
                )
            } else {
                RedisHealthStatus(
                    status = "DOWN",
                    responseTimeMs = responseTime,
                    error = "Unexpected response: $pong"
                )
            }
        } catch (e: Exception) {
            RedisHealthStatus(
                status = "DOWN",
                responseTimeMs = -1,
                error = e.message
            )
        }
    }

    data class RedisHealthStatus(
        val status: String,
        val responseTimeMs: Long,
        val error: String? = null
    )
}
