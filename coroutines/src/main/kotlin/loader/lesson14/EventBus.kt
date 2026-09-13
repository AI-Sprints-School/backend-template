package loader.lesson14

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import loader.core.FakeJournal
import java.util.concurrent.atomic.AtomicInteger

// Урок 14. Шина событий загрузчика на SharedFlow.

sealed interface DownloadEvent {
    data class Started(val file: String) : DownloadEvent
    data class Completed(val file: String, val ms: Long) : DownloadEvent
    data class Failed(val file: String, val reason: String) : DownloadEvent
}

class EventBus(
    replay: Int = 1,
    extraBufferCapacity: Int = 16,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
) {
    /** События шины для подписчиков — только чтение. */
    val events: SharedFlow<DownloadEvent>
        get() = TODO("Урок 14: отдать события шины как SharedFlow")

    /** Публикация; при полном буфере и SUSPEND ждёт место. */
    suspend fun publish(event: DownloadEvent) {
        TODO("Урок 14: опубликовать событие через emit")
    }

    /** Публикация без ожидания; false — буфер полон. */
    fun tryPublish(event: DownloadEvent): Boolean {
        TODO("Урок 14: опубликовать событие без ожидания через tryEmit")
    }
}

/** Готово в шаблоне: счётчики статистики. */
class DownloadStats {
    private val completedCount = AtomicInteger()
    private val failedCount = AtomicInteger()

    val completed: Int get() = completedCount.get()
    val failed: Int get() = failedCount.get()

    fun onCompleted() { completedCount.incrementAndGet() }
    fun onFailed() { failedCount.incrementAndGet() }
}

/** Подписчик-журнал: каждое событие шины — строкой в journal. */
fun CoroutineScope.launchJournal(bus: EventBus, journal: FakeJournal): Job {
    TODO("Урок 14: подписать журнал на события шины")
}

/** Подписчик-статистика: Completed → stats.onCompleted(), Failed → stats.onFailed(). */
fun CoroutineScope.launchStats(bus: EventBus, stats: DownloadStats): Job {
    TODO("Урок 14: подписать статистику на события шины")
}
