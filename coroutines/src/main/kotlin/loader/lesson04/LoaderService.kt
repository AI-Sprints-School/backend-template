package loader.lesson04

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.log

// Урок 4. Сервис со своим scope: start и stop.

class LoaderService(
    private val catalog: SourceCatalog,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val intervalMs: Long = 1000,
) {
    // Scope — поле сервиса: CoroutineScope(dispatcher + Job())

    /** По корутине на файл в scope сервиса: download, пауза intervalMs, повтор. После stop — IllegalStateException("сервис остановлен"). */
    fun start(files: List<RemoteFile>) {
        TODO("Урок 04: запустить загрузку каждого файла в scope сервиса")
    }

    /** log("stop") и отмена scope сервиса. */
    fun stop() {
        TODO("Урок 04: остановить сервис отменой его scope")
    }

    val isRunning: Boolean
        get() = TODO("Урок 04: сказать, работает ли сервис")
}
