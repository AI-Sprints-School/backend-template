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
import org.springframework.util.ClassUtils
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Основа всех тестов шаблона: полный контекст Spring на случайном порту, профиль `test`,
 * PostgreSQL 18 в Testcontainers (одна база на весь прогон). С урока 22, когда в сборке
 * появился стартер Kafka, рядом поднимается брокер `apache/kafka:4.2.2` — тот же образ, что
 * в Compose; до урока 22 брокер не запускается и прогон глав 1–5 его не ждёт. Контекст кешируется, поэтому
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

        /** Стартер Kafka раскомментирован (урок 22) — в тестовом classpath есть Spring for Apache Kafka. */
        val kafkaEnabled: Boolean = ClassUtils.isPresent("org.springframework.kafka.core.KafkaTemplate", null)

        /** Брокер для тестов главы 6; до урока 22 — null. */
        val kafka: KafkaContainer? = if (kafkaEnabled) KafkaContainer("apache/kafka:4.2.2").apply { start() } else null

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            kafka?.let { registry.add("spring.kafka.bootstrap-servers", it::getBootstrapServers) }
        }
    }
}
