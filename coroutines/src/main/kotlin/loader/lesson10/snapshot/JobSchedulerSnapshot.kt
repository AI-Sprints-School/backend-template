package loader.lesson10.snapshot

// Снимок фрагмента JobScheduler.kt из эталонного сервиса школы
// (ветка modernize-2026-09, коммит e4bbc8a), сокращён для урока 10.

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface TokenRepository {
    // В эталоне: transaction { RefreshTokens.deleteWhere { … } } — Exposed поверх JDBC
    fun deleteExpiredRefreshTokens(): Int
}

class JobSchedulerSnapshot(private val tokenRepository: TokenRepository) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isRunning = AtomicBoolean(false)

    fun start() {
        if (isRunning.getAndSet(true)) return
        scope.launch { runTokenCleanupJob() }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        scope.cancel()
    }

    private suspend fun runTokenCleanupJob() {
        while (isRunning.get()) {
            try {
                delay(TOKEN_CLEANUP_INTERVAL)
                if (!isRunning.get()) break
                val deleted = tokenRepository.deleteExpiredRefreshTokens()
                println("Очистка токенов: удалено $deleted")
            } catch (e: CancellationException) {
                println("Задача очистки токенов отменена")
                break
            } catch (e: Exception) {
                println("Ошибка при очистке токенов: ${e.message}")
            }
        }
    }

    private companion object {
        const val TOKEN_CLEANUP_INTERVAL = 3_600_000L
    }
}
