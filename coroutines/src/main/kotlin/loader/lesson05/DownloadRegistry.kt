package loader.lesson05

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import loader.core.RemoteFile
import loader.core.SourceCatalog

// Урок 5. Реестр загрузок: Job каждой загрузки, отмена одной, состояния.

enum class DownloadState { ACTIVE, COMPLETED, CANCELLED }

class DownloadRegistry(private val scope: CoroutineScope, private val catalog: SourceCatalog) {

    /** Запускает загрузку файла в переданном scope и запоминает её Job под id. */
    fun start(id: String, file: RemoteFile): Job {
        TODO("Урок 05: запустить загрузку в scope и запомнить её Job")
    }

    /** Отменяет загрузку; false — нет такой или она уже завершена. */
    fun cancel(id: String): Boolean {
        TODO("Урок 05: отменить одну загрузку по id")
    }

    /** Ждёт все загрузки реестра. */
    suspend fun awaitAll() {
        TODO("Урок 05: дождаться всех загрузок через joinAll")
    }

    /** Состояние каждой загрузки — из флагов Job: isActive, isCancelled, isCompleted. */
    fun states(): Map<String, DownloadState> {
        TODO("Урок 05: вывести состояния загрузок из флагов Job")
    }
}
