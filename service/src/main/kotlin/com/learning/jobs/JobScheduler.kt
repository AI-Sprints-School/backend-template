package com.learning.jobs

import com.learning.repositories.TokenRepository
import com.learning.services.FileStorageService
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Планировщик фоновых задач
 */
class JobScheduler(
    private val tokenRepository: TokenRepository,
    private val fileStorageService: FileStorageService,
) {
    private val logger = LoggerFactory.getLogger(JobScheduler::class.java)
    // Свой scope у каждого экземпляра: после stop() отменённый scope не переиспользуется
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRunning = AtomicBoolean(false)

    companion object {
        // Интервалы выполнения (в миллисекундах)
        private const val TOKEN_CLEANUP_INTERVAL = 3600_000L // 1 час
        private const val FILE_CLEANUP_INTERVAL = 86400_000L // 24 часа
        private const val FILE_MAX_AGE = 7 * 24 * 3600_000L // 7 дней для временных файлов
    }

    /**
     * Запуск планировщика
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            logger.warn("JobScheduler уже запущен")
            return
        }

        logger.info("Запуск JobScheduler...")

        // Задача очистки токенов
        scope.launch {
            runTokenCleanupJob()
        }

        // Задача очистки файлов (опционально)
        scope.launch {
            runFileCleanupJob()
        }

        logger.info("JobScheduler запущен")
    }

    /**
     * Остановка планировщика
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            logger.warn("JobScheduler уже остановлен")
            return
        }

        logger.info("Остановка JobScheduler...")
        scope.cancel()
        logger.info("JobScheduler остановлен")
    }

    /**
     * Задача очистки expired токенов
     */
    private suspend fun runTokenCleanupJob() {
        while (isRunning.get()) {
            try {
                delay(TOKEN_CLEANUP_INTERVAL)
                
                if (!isRunning.get()) break

                logger.debug("Запуск очистки expired токенов...")

                val deletedEmailTokens = tokenRepository.deleteExpiredEmailVerificationTokens()
                val deletedPasswordTokens = tokenRepository.deleteExpiredPasswordResetTokens()
                val deletedRefreshTokens = tokenRepository.deleteExpiredRefreshTokens()

                val totalDeleted = deletedEmailTokens + deletedPasswordTokens + deletedRefreshTokens

                if (totalDeleted > 0) {
                    logger.info(
                        "Очистка токенов завершена. Удалено: " +
                        "email verification: $deletedEmailTokens, " +
                        "password reset: $deletedPasswordTokens, " +
                        "refresh: $deletedRefreshTokens"
                    )
                } else {
                    logger.debug("Очистка токенов завершена. Нет expired токенов.")
                }
            } catch (e: CancellationException) {
                logger.info("Token cleanup job отменен")
                break
            } catch (e: Exception) {
                logger.error("Ошибка при очистке токенов: ${e.message}", e)
            }
        }
    }

    /**
     * Задача очистки старых файлов
     */
    private suspend fun runFileCleanupJob() {
        while (isRunning.get()) {
            try {
                delay(FILE_CLEANUP_INTERVAL)
                
                if (!isRunning.get()) break

                logger.debug("Запуск очистки старых файлов...")

                val deletedFiles = fileStorageService.cleanupOldFiles(FILE_MAX_AGE)

                if (deletedFiles > 0) {
                    logger.info("Очистка файлов завершена. Удалено файлов: $deletedFiles")
                } else {
                    logger.debug("Очистка файлов завершена. Нет старых файлов.")
                }
            } catch (e: CancellationException) {
                logger.info("File cleanup job отменен")
                break
            } catch (e: Exception) {
                logger.error("Ошибка при очистке файлов: ${e.message}", e)
            }
        }
    }

    /**
     * Немедленный запуск очистки токенов
     */
    fun runTokenCleanupNow(): CleanupResult {
        return try {
            val deletedEmailTokens = tokenRepository.deleteExpiredEmailVerificationTokens()
            val deletedPasswordTokens = tokenRepository.deleteExpiredPasswordResetTokens()
            val deletedRefreshTokens = tokenRepository.deleteExpiredRefreshTokens()

            CleanupResult(
                success = true,
                deletedEmailVerificationTokens = deletedEmailTokens,
                deletedPasswordResetTokens = deletedPasswordTokens,
                deletedRefreshTokens = deletedRefreshTokens
            )
        } catch (e: Exception) {
            logger.error("Ошибка при немедленной очистке токенов: ${e.message}", e)
            CleanupResult(success = false, error = e.message)
        }
    }

    /**
     * Немедленный запуск очистки файлов
     */
    fun runFileCleanupNow(maxAgeMillis: Long = FILE_MAX_AGE): Int {
        return try {
            fileStorageService.cleanupOldFiles(maxAgeMillis)
        } catch (e: Exception) {
            logger.error("Ошибка при немедленной очистке файлов: ${e.message}", e)
            0
        }
    }

    /**
     * Проверка статуса планировщика
     */
    fun isActive(): Boolean = isRunning.get()

    data class CleanupResult(
        val success: Boolean,
        val deletedEmailVerificationTokens: Int = 0,
        val deletedPasswordResetTokens: Int = 0,
        val deletedRefreshTokens: Int = 0,
        val error: String? = null
    ) {
        val totalDeleted: Int
            get() = deletedEmailVerificationTokens + deletedPasswordResetTokens + deletedRefreshTokens
    }
}
