package com.learning.achievements.support

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Отправка событий в топик обычным клиентом Kafka — так, как их шлёт service-spring: ключ `userId`,
 * значение — JSON без заголовков типа. И чтение топика недоставленных. Готово в шаблоне.
 */
class TestEvents(private val bootstrapServers: String) {

    private val producer = KafkaProducer<String, String>(
        mapOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers),
        StringSerializer(),
        StringSerializer(),
    )

    fun send(key: String, json: String) {
        producer.send(ProducerRecord(TOPIC, key, json)).get()
    }

    fun send(event: Event) = send(event.userId.toString(), event.json())

    data class Event(
        val userId: UUID = UUID.randomUUID(),
        val courseId: UUID = UUID.randomUUID(),
        val completedLessons: Int,
        val totalLessons: Int,
        val eventId: UUID = UUID.randomUUID(),
        val lessonId: UUID = UUID.randomUUID(),
    ) {
        fun json() = """{"eventId":"$eventId","userId":"$userId","courseId":"$courseId","lessonId":"$lessonId",""" +
            """"completedLessons":$completedLessons,"totalLessons":$totalLessons,"occurredAt":"${Instant.parse("2026-10-31T10:15:30Z")}"}"""
    }

    /** Читатель топика недоставленных с текущего конца. */
    fun deadLetters(): Recorder = Recorder(DLT)

    inner class Recorder(private val topic: String) : AutoCloseable {
        private val consumer = KafkaConsumer<String, String>(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ),
            StringDeserializer(),
            StringDeserializer(),
        )
        private val received = mutableListOf<ConsumerRecord<String, String>>()

        init {
            val partitions = consumer.partitionsFor(topic, Duration.ofSeconds(30)).map { TopicPartition(topic, it.partition()) }
            consumer.assign(partitions)
            consumer.seekToEnd(partitions)
            partitions.forEach { consumer.position(it) }
        }

        fun await(timeout: Duration = Duration.ofSeconds(30), until: (ConsumerRecord<String, String>) -> Boolean): ConsumerRecord<String, String> {
            val deadline = System.nanoTime() + timeout.toNanos()
            while (true) {
                received.firstOrNull(until)?.let { return it }
                check(System.nanoTime() < deadline) { "За ${timeout.seconds} с в $topic не появилось нужной записи; пришло: ${received.map { it.value() }}" }
                consumer.poll(Duration.ofMillis(250)).forEach { received += it }
            }
        }

        override fun close() = consumer.close()
    }

    companion object {
        const val TOPIC = "lesson-completed"
        const val DLT = "lesson-completed-dlt"
    }
}
