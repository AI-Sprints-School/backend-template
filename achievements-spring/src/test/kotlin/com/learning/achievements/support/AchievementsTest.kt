package com.learning.achievements.support

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.ExecutionException

/**
 * Основа тестов сервиса достижений: полный контекст, PostgreSQL 18 и брокер `apache/kafka:4.2.2`
 * в Testcontainers (по одному на прогон), адреса подключает `@ServiceConnection` — без
 * `@DynamicPropertySource`. Топики `lesson-completed` и `lesson-completed-dlt` (по 3 партиции)
 * создаются до старта контекста. Перед каждым тестом — ожидание, пока консьюмер получит партиции:
 * иначе событие, отправленное раньше назначения, консьюмер с `latest` не увидит.
 * Готово в шаблоне, править не нужно.
 */
@SpringBootTest
abstract class AchievementsTest {

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var registry: KafkaListenerEndpointRegistry

    protected val events: TestEvents by lazy { TestEvents(kafka.bootstrapServers) }

    @BeforeEach
    fun waitForConsumer() {
        val containers = registry.allListenerContainers.filter { container ->
            container.containerProperties.topics.orEmpty().contains(TestEvents.TOPIC)
        }
        check(containers.isNotEmpty()) { "Урок 24: нет @KafkaListener на топик ${TestEvents.TOPIC}" }
        containers.forEach { ContainerTestUtils.waitForAssignment(it, PARTITIONS) }
    }

    /** Достижения студента по курсу в схеме achievements. */
    protected fun achievements(userId: Any, courseId: Any): Int =
        jdbc.queryForObject("SELECT count(*) FROM achievements.achievements WHERE user_id = ? AND course_id = ?", Int::class.java, userId, courseId)!!

    protected fun processed(eventId: Any): Int =
        jdbc.queryForObject("SELECT count(*) FROM achievements.processed_events WHERE event_id = ?", Int::class.java, eventId)!!

    companion object {
        const val PARTITIONS = 3

        @JvmField
        @ServiceConnection
        val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:18-alpine").apply { start() }

        @JvmField
        @ServiceConnection
        val kafka: KafkaContainer = KafkaContainer("apache/kafka:4.2.2").apply {
            start()
            AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers)).use { admin ->
                listOf(TestEvents.TOPIC, TestEvents.DLT).forEach { name ->
                    try {
                        admin.createTopics(listOf(NewTopic(name, PARTITIONS, 1.toShort()))).all().get()
                    } catch (e: ExecutionException) {
                        if (e.cause !is TopicExistsException) throw e
                    }
                }
            }
        }
    }
}
