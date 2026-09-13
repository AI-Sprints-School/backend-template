package loader.lesson01

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import loader.core.Payload
import loader.core.RemoteFile
import loader.core.SourceCatalog

// Урок 1. Три режима загрузки. Источник файла: catalog.source(file.sourceId).

/** Загружает файлы по очереди через download и возвращает Payload в порядке файлов. */
suspend fun loadSequential(files: List<RemoteFile>, catalog: SourceCatalog): List<Payload> {
    TODO("Урок 01: загрузить файлы по очереди через download")
}

/** Запускает загрузку каждого файла через launch внутри coroutineScope; возвращается после всех загрузок. */
suspend fun loadConcurrent(files: List<RemoteFile>, catalog: SourceCatalog) {
    TODO("Урок 01: загрузить файлы параллельно через launch и download")
}

/** То же, что loadConcurrent, но через downloadBlocking — эксперимент с блокировкой. */
suspend fun loadConcurrentBlocking(files: List<RemoteFile>, catalog: SourceCatalog) {
    TODO("Урок 01: загрузить файлы через launch и downloadBlocking")
}
