package com.learning

import com.learning.integration.DatabaseTestBase
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication

/**
 * Поднимает приложение целиком — те же модули из application.yaml, что и в
 * проде, — поверх базы из Testcontainers. Запросы идут в движок Ktor в памяти,
 * без сети и без заранее запущенного сервера.
 */
fun DatabaseTestBase.apiTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    environment {
        config = ApplicationConfig("application.yaml").mergeWith(
            MapApplicationConfig(
                "database.url" to DatabaseTestBase.postgresContainer.jdbcUrl,
                "database.user" to DatabaseTestBase.postgresContainer.username,
                "database.password" to DatabaseTestBase.postgresContainer.password,
                "database.seed" to "false",
                "redis.enabled" to "false",
                "jobs.enabled" to "false",
            )
        )
    }
    block()
}
