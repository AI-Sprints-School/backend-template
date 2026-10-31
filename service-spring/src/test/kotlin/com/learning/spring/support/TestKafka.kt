package com.learning.spring.support

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.UUID

/**
 * Чтение топика в тестах главы 6 обычным клиентом Kafka, мимо кода сервиса: тест видит ровно то,
 * что легло в брокер. `recorder` запоминает конец топика до действия, `awaitRecords` читает то,
 * что пришло после. Готово в шаблоне.
 */
object TestKafka {

    const val TOPIC = "lesson-completed"

    /** Адрес брокера Testcontainers; до урока 22 — понятное падение вместо таймаута. */
    fun bootstrapServers(): String {
        check(IntegrationTest.kafkaEnabled) {
            "Урок 22: раскомментируйте стартер spring-boot-starter-kafka в service-spring/build.gradle.kts"
        }
        return IntegrationTest.kafka!!.bootstrapServers
    }

    fun partitions(topic: String = TOPIC): Int =
        AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers())).use { admin ->
            admin.describeTopics(listOf(topic)).allTopicNames().get()[topic]!!.partitions().size
        }

    /** Читатель топика с текущего конца: всё, что было в топике до вызова, он не увидит. */
    fun recorder(topic: String = TOPIC): Recorder = Recorder(topic)

    class Recorder(private val topic: String) : AutoCloseable {
        private val consumer = KafkaConsumer<String, String>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ),
            StringDeserializer(),
            StringDeserializer(),
        )
        private val received = mutableListOf<ConsumerRecord<String, String>>()

        init {
            val partitions = consumer.partitionsFor(topic, Duration.ofSeconds(30))
            check(!partitions.isNullOrEmpty()) { "Урок 22: топика $topic нет — объявите его бином NewTopic" }
            val assigned = partitions.map { TopicPartition(topic, it.partition()) }
            consumer.assign(assigned)
            consumer.seekToEnd(assigned)
            assigned.forEach { consumer.position(it) } // фиксирует конец топика до действия теста
        }

        /** Ждёт, пока среди новых записей появится подходящая под `until`, и возвращает все новые записи. */
        fun awaitRecords(timeout: Duration = Duration.ofSeconds(20), until: (ConsumerRecord<String, String>) -> Boolean): List<ConsumerRecord<String, String>> {
            val deadline = System.nanoTime() + timeout.toNanos()
            while (received.none(until)) {
                check(System.nanoTime() < deadline) { "За ${timeout.seconds} с в топике $topic не появилось нужной записи; пришло: ${received.map { it.value() }}" }
                consumer.poll(Duration.ofMillis(250)).forEach { received += it }
            }
            return received.toList()
        }

        override fun close() = consumer.close()
    }
}
