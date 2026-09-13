package com.learning.integration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Базовый класс для интеграционных тестов с PostgreSQL через Testcontainers
 */
@Testcontainers
abstract class DatabaseTestBase {

    companion object {
        @Container
        @JvmStatic
        val postgresContainer: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)

        private var dataSource: HikariDataSource? = null
        private lateinit var database: Database

        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            postgresContainer.start()

            val config = HikariConfig().apply {
                jdbcUrl = postgresContainer.jdbcUrl
                username = postgresContainer.username
                password = postgresContainer.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                connectionTimeout = 10000
                idleTimeout = 60000
                maxLifetime = 120000
                connectionTestQuery = "SELECT 1"
                poolName = "TestPool"
            }

            dataSource = HikariDataSource(config)

            // Схема — те же миграции Flyway, что у приложения
            Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate()

            database = Database.connect(dataSource!!)
        }

        @AfterAll
        @JvmStatic
        fun tearDownDatabase() {
            dataSource?.close()
        }
    }

    @BeforeEach
    fun cleanupTables() {
        transaction(database) {
            // Clean tables in reverse dependency order
            exec("TRUNCATE TABLE user_quiz_answers CASCADE")
            exec("TRUNCATE TABLE user_quiz_sessions CASCADE")
            exec("TRUNCATE TABLE user_progress CASCADE")
            exec("TRUNCATE TABLE roadmap_steps CASCADE")
            exec("TRUNCATE TABLE roadmaps CASCADE")
            exec("TRUNCATE TABLE interview_questions CASCADE")
            exec("TRUNCATE TABLE sprint_courses CASCADE")
            exec("TRUNCATE TABLE sprints CASCADE")
            exec("TRUNCATE TABLE quiz_questions CASCADE")
            exec("TRUNCATE TABLE quizzes CASCADE")
            exec("TRUNCATE TABLE lessons CASCADE")
            exec("TRUNCATE TABLE courses CASCADE")
            exec("TRUNCATE TABLE refresh_tokens CASCADE")
            exec("TRUNCATE TABLE password_reset_tokens CASCADE")
            exec("TRUNCATE TABLE email_verification_tokens CASCADE")
            exec("TRUNCATE TABLE users CASCADE")
        }
    }
}
