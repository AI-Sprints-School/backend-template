package loader.lesson13

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import loader.core.RemoteFile
import loader.core.SourceCatalog

// Урок 13. Состояние загрузчика в StateFlow.

enum class Status { IDLE, RUNNING, STOPPING, STOPPED }

data class LoaderSnapshot(val status: Status, val completed: Int, val failed: Int)

class StatefulLoader(scope: CoroutineScope, private val catalog: SourceCatalog) {

    /** Текущее состояние; начальное — IDLE, 0 готовых, 0 сбоев. Все изменения — через MutableStateFlow.update. */
    val state: StateFlow<LoaderSnapshot>
        get() = TODO("Урок 13: отдать состояние загрузчика как StateFlow")

    /** IDLE → RUNNING и загрузка файлов в scope; после каждой — completed или failed + 1. Из другого статуса — IllegalStateException. */
    fun start(files: List<RemoteFile>) {
        TODO("Урок 13: запустить загрузки и перевести состояние в RUNNING")
    }

    /** RUNNING → STOPPING → отмена и join задач → STOPPED. */
    suspend fun stop() {
        TODO("Урок 13: остановить загрузки и выставить STOPPED после их завершения")
    }
}
