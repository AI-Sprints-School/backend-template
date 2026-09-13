package com.learning.services

import com.learning.config.EmailConfig
import com.learning.config.EnvironmentConfig
import jakarta.mail.Message
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.email.Recipient
import org.simplejavamail.email.EmailBuilder
import org.slf4j.LoggerFactory

/**
 * Сервис отправки email
 */
class EmailService {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    private val baseUrl: String
        get() = if (EnvironmentConfig.isProduction) {
            "https://your-domain.com" // TODO: добавить в конфигурацию
        } else {
            "http://localhost:${EnvironmentConfig.serverPort}"
        }

    /**
     * Отправка email для подтверждения email адреса
     */
    fun sendVerificationEmail(to: String, token: String, firstName: String): Boolean {
        if (!EmailConfig.isConfigured) {
            logger.warn("Email не настроен, пропускаем отправку verification email для $to")
            return false
        }

        val verificationLink = "$baseUrl/api/v1/auth/verify-email?token=$token"

        val email = EmailBuilder.startingBlank()
            .from(EmailConfig.fromEmail)
            .withRecipients(Recipient(null, to, Message.RecipientType.TO, null))
            .withSubject("Подтвердите ваш email - Learning Platform")
            .withHTMLText(buildVerificationEmailHtml(firstName, verificationLink))
            .withPlainText(buildVerificationEmailText(firstName, verificationLink))
            .buildEmail()

        return sendEmail(email)
    }

    /**
     * Отправка email для сброса пароля
     */
    fun sendPasswordResetEmail(to: String, token: String, firstName: String): Boolean {
        if (!EmailConfig.isConfigured) {
            logger.warn("Email не настроен, пропускаем отправку password reset email для $to")
            return false
        }

        val resetLink = "$baseUrl/reset-password?token=$token"

        val email = EmailBuilder.startingBlank()
            .from(EmailConfig.fromEmail)
            .withRecipients(Recipient(null, to, Message.RecipientType.TO, null))
            .withSubject("Сброс пароля - Learning Platform")
            .withHTMLText(buildPasswordResetEmailHtml(firstName, resetLink))
            .withPlainText(buildPasswordResetEmailText(firstName, resetLink))
            .buildEmail()

        return sendEmail(email)
    }

    /**
     * Отправка общего email
     */
    fun sendEmail(to: String, subject: String, htmlBody: String, textBody: String): Boolean {
        if (!EmailConfig.isConfigured) {
            logger.warn("Email не настроен, пропускаем отправку email для $to")
            return false
        }

        val email = EmailBuilder.startingBlank()
            .from(EmailConfig.fromEmail)
            .withRecipients(Recipient(null, to, Message.RecipientType.TO, null))
            .withSubject(subject)
            .withHTMLText(htmlBody)
            .withPlainText(textBody)
            .buildEmail()

        return sendEmail(email)
    }

    private fun sendEmail(email: Email): Boolean {
        return try {
            val mailer = EmailConfig.getMailer()
            if (mailer == null) {
                logger.error("Mailer не инициализирован")
                return false
            }

            mailer.sendMail(email)
            logger.info("Email успешно отправлен на ${email.recipients.firstOrNull()?.address}")
            true
        } catch (e: Exception) {
            logger.error("Ошибка отправки email: ${e.message}", e)
            false
        }
    }

    // ==================== Email Templates ====================

    private fun buildVerificationEmailHtml(firstName: String, link: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background-color: #4F46E5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .button:hover { background-color: #4338CA; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #6b7280; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Learning Platform</h1>
                    </div>
                    <div class="content">
                        <h2>Здравствуйте, $firstName!</h2>
                        <p>Спасибо за регистрацию на нашей платформе. Для завершения регистрации подтвердите ваш email адрес.</p>
                        <p style="text-align: center;">
                            <a href="$link" class="button">Подтвердить email</a>
                        </p>
                        <p>Или скопируйте ссылку в браузер:</p>
                        <p style="word-break: break-all; background: #e5e7eb; padding: 10px; border-radius: 4px; font-size: 12px;">$link</p>
                        <p>Ссылка действительна в течение 24 часов.</p>
                        <p>Если вы не регистрировались на нашей платформе, проигнорируйте это письмо.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Learning Platform. Все права защищены.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildVerificationEmailText(firstName: String, link: String): String {
        return """
            Здравствуйте, $firstName!
            
            Спасибо за регистрацию на Learning Platform.
            
            Для подтверждения email перейдите по ссылке:
            $link
            
            Ссылка действительна в течение 24 часов.
            
            Если вы не регистрировались на нашей платформе, проигнорируйте это письмо.
            
            ---
            Learning Platform
        """.trimIndent()
    }

    private fun buildPasswordResetEmailHtml(firstName: String, link: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #DC2626; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background-color: #f9fafb; padding: 30px; border-radius: 0 0 8px 8px; }
                    .button { display: inline-block; background-color: #DC2626; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                    .button:hover { background-color: #B91C1C; }
                    .warning { background-color: #FEF3C7; border-left: 4px solid #F59E0B; padding: 10px 15px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #6b7280; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Сброс пароля</h1>
                    </div>
                    <div class="content">
                        <h2>Здравствуйте, $firstName!</h2>
                        <p>Мы получили запрос на сброс пароля для вашего аккаунта.</p>
                        <p style="text-align: center;">
                            <a href="$link" class="button">Сбросить пароль</a>
                        </p>
                        <p>Или скопируйте ссылку в браузер:</p>
                        <p style="word-break: break-all; background: #e5e7eb; padding: 10px; border-radius: 4px; font-size: 12px;">$link</p>
                        <div class="warning">
                            <strong>Важно:</strong> Ссылка действительна в течение 1 часа.
                        </div>
                        <p>Если вы не запрашивали сброс пароля, проигнорируйте это письмо. Ваш пароль останется прежним.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Learning Platform. Все права защищены.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildPasswordResetEmailText(firstName: String, link: String): String {
        return """
            Здравствуйте, $firstName!
            
            Мы получили запрос на сброс пароля для вашего аккаунта.
            
            Для сброса пароля перейдите по ссылке:
            $link
            
            ВАЖНО: Ссылка действительна в течение 1 часа.
            
            Если вы не запрашивали сброс пароля, проигнорируйте это письмо.
            
            ---
            Learning Platform
        """.trimIndent()
    }
}
