package com.learning.config

import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.mailer.MailerBuilder
import org.slf4j.LoggerFactory

/**
 * Конфигурация email сервиса
 */
object EmailConfig {
    private val logger = LoggerFactory.getLogger(EmailConfig::class.java)

    private var mailer: Mailer? = null

    val host: String get() = EnvironmentConfig.smtpHost
    val port: Int get() = EnvironmentConfig.smtpPort
    val username: String get() = EnvironmentConfig.smtpUsername
    val password: String get() = EnvironmentConfig.smtpPassword
    val fromEmail: String get() = EnvironmentConfig.smtpFrom

    val isConfigured: Boolean
        get() = username.isNotBlank() && password.isNotBlank()

    /**
     * Инициализация Mailer
     */
    fun init(): Mailer? {
        if (!isConfigured) {
            logger.warn("Email не настроен: SMTP_USERNAME или SMTP_PASSWORD не указаны")
            return null
        }

        return try {
            mailer = MailerBuilder
                .withSMTPServer(host, port, username, password)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withSessionTimeout(10_000)
                .withDebugLogging(EnvironmentConfig.isDevelopment)
                .buildMailer()

            logger.info("Email mailer инициализирован: $host:$port")
            mailer
        } catch (e: Exception) {
            logger.error("Ошибка инициализации email mailer: ${e.message}", e)
            null
        }
    }

    fun getMailer(): Mailer? {
        if (mailer == null) {
            init()
        }
        return mailer
    }

    /**
     * Проверка подключения к SMTP серверу
     */
    fun testConnection(): Boolean {
        return try {
            val testMailer = getMailer()
            testMailer?.testConnection() ?: false
            true
        } catch (e: Exception) {
            logger.error("Ошибка проверки SMTP подключения: ${e.message}")
            false
        }
    }
}
