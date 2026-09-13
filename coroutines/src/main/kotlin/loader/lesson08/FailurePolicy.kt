package loader.lesson08

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import loader.core.Payload
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.log
import java.io.IOException

// Урок 8. Политика сбоев сервиса: обработчик корня, scope сервиса, загрузка без исключения наружу.

/** Обработчик корня: log("сбой задачи: <сообщение исключения>"). */
fun rootHandler(): CoroutineExceptionHandler {
    TODO("Урок 08: создать обработчик корня, который пишет сбой в лог")
}

/** Scope сервиса: Job() + Dispatchers.Default + handler. */
fun serviceScope(handler: CoroutineExceptionHandler): CoroutineScope {
    TODO("Урок 08: собрать scope сервиса с обработчиком")
}

/** Загрузка файла; null при сбое источника (IOException). Отмену не глотает. */
suspend fun loadOrNull(file: RemoteFile, catalog: SourceCatalog): Payload? {
    TODO("Урок 08: вернуть null при сбое источника, не глотая отмену")
}
