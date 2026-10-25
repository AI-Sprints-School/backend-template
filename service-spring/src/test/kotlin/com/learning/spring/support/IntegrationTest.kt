package com.learning.spring.support

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Основа всех тестов шаблона: полный контекст Spring на случайном порту, профиль `test`,
 * PostgreSQL 18 в Testcontainers (одна база на весь прогон). Контекст кешируется, поэтому
 * класс-наследник не должен менять конфигурацию (`@MockitoBean`, `@TestPropertySource`):
 * это поднимет второй контекст и замедлит прогон — тема урока 18.
 *
 * Тесты шаблона ходят в сервис по HTTP и в базу через JDBC: так они компилируются на чистом
 * шаблоне, где нет ни сущностей, ни контроллеров, и зеленеют по мере уроков.
 * Готово в шаблоне, править не нужно.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @Autowired
    protected lateinit var environment: Environment

    @LocalServerPort
    private var port: Int = 0

    protected lateinit var http: TestHttp
    protected lateinit var data: TestData
    protected lateinit var users: TestUsers

    @BeforeEach
    fun setUpSupport() {
        http = TestHttp("http://localhost:$port")
        data = TestData(jdbc)
        users = TestUsers(jdbc, environment)
    }

    companion object {
        // Один контейнер на JVM: Ryuk снимет его при выходе
        private val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
