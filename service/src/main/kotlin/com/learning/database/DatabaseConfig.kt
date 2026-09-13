package com.learning.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.slf4j.LoggerFactory

/**
 * Настройки подключения к базе — секция `database` файла application.yaml.
 */
data class DatabaseSettings(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int = 10,
    /** Накатить учебные данные из classpath:db/seed (только для локальной разработки). */
    val seed: Boolean = false,
)

object DatabaseConfig {

    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    private var dataSource: HikariDataSource? = null
    private var database: Database? = null

    /**
     * Пул соединений → миграции Flyway → подключение Exposed.
     * Схема живёт только в миграциях `src/main/resources/db/migration`,
     * приложение её не создаёт и не правит.
     */
    fun init(settings: DatabaseSettings) {
        try {
            logger.info("Инициализация подключения к базе данных: {}", settings.url)

            val config = HikariConfig().apply {
                jdbcUrl = settings.url
                username = settings.user
                password = settings.password
                driverClassName = "org.postgresql.Driver"

                maximumPoolSize = settings.maxPoolSize
                minimumIdle = 2
                connectionTimeout = 30000
                idleTimeout = 600000
                maxLifetime = 1800000
                initializationFailTimeout = 30000
                connectionTestQuery = "SELECT 1"
                validationTimeout = 5000
                poolName = "LearningPlatformPool"
            }

            val ds = HikariDataSource(config)
            dataSource = ds

            val locations = buildList {
                add("classpath:db/migration")
                if (settings.seed) add("classpath:db/seed")
            }
            val result = Flyway.configure()
                .dataSource(ds)
                .locations(*locations.toTypedArray())
                .load()
                .migrate()
            logger.info("Миграции применены: {}, версия схемы: {}", result.migrationsExecuted, result.targetSchemaVersion)

            database = Database.connect(ds)
            logger.info("Подключение к базе данных успешно установлено")
        } catch (e: Exception) {
            logger.error("Критическая ошибка: не удалось подключиться к базе данных: ${e.message}", e)
            throw RuntimeException("Не удалось подключиться к базе данных.", e)
        }
    }

    fun getDataSource(): HikariDataSource? = dataSource

    fun getConnection(): java.sql.Connection {
        return dataSource?.connection ?: throw IllegalStateException("Database is not initialized")
    }

    /**
     * Закрытие подключения к базе данных
     */
    fun close() {
        try {
            database?.let { TransactionManager.closeAndUnregister(it) }
            database = null
            dataSource?.close()
            logger.info("Подключение к базе данных закрыто")
        } catch (e: Exception) {
            logger.error("Ошибка при закрытии подключения к БД: ${e.message}", e)
        }
    }

    /**
     * Проверка подключения к базе данных
     */
    fun checkConnection(): DatabaseHealthStatus {
        return try {
            val startTime = System.currentTimeMillis()
            dataSource?.connection?.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                    }
                }
            }
            val responseTime = System.currentTimeMillis() - startTime
            
            val poolStats = dataSource?.let {
                PoolStats(
                    activeConnections = it.hikariPoolMXBean?.activeConnections ?: 0,
                    idleConnections = it.hikariPoolMXBean?.idleConnections ?: 0,
                    totalConnections = it.hikariPoolMXBean?.totalConnections ?: 0,
                    threadsAwaitingConnection = it.hikariPoolMXBean?.threadsAwaitingConnection ?: 0
                )
            }

            DatabaseHealthStatus(
                status = "UP",
                responseTimeMs = responseTime,
                poolStats = poolStats
            )
        } catch (e: Exception) {
            logger.error("Database health check failed", e)
            DatabaseHealthStatus(
                status = "DOWN",
                responseTimeMs = -1,
                error = e.message
            )
        }
    }

    data class DatabaseHealthStatus(
        val status: String,
        val responseTimeMs: Long,
        val poolStats: PoolStats? = null,
        val error: String? = null
    )

    data class PoolStats(
        val activeConnections: Int,
        val idleConnections: Int,
        val totalConnections: Int,
        val threadsAwaitingConnection: Int
    )
}
