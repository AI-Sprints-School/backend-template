package loader.lesson01

import kotlinx.coroutines.runBlocking
import loader.core.Log
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 1 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=01

fun main() = lessonMain {
    runBlocking {
        mode(1, "подряд") { files, catalog -> loadSequential(files, catalog) }
        mode(2, "параллельно, приостановка") { files, catalog -> loadConcurrent(files, catalog) }
        mode(3, "параллельно, блокировка") { files, catalog -> loadConcurrentBlocking(files, catalog) }

        Log.plain("режим 4: 10 000 загрузок по 0,4 с, приостановка")
        val quiet = SourceCatalog(quiet = true)
        val millis = measureTimeMillis { loadConcurrent(quiet.manyFiles(10_000), quiet) }
        Log.plain("режим 4 занял ${seconds(millis)} с, потоков: ${quiet.source("alpha").stats.threadNames.size}")
    }
}

private suspend fun mode(number: Int, title: String, load: suspend (List<RemoteFile>, SourceCatalog) -> Unit) {
    val catalog = SourceCatalog()
    val files = listOf("a.csv", "b.csv", "c.csv").map(catalog::file)
    Log.plain("режим $number: $title")
    val millis = measureTimeMillis { load(files, catalog) }
    val threads = files.flatMap { catalog.source(it.sourceId).stats.threadNames }.toSet()
    Log.plain("режим $number занял ${seconds(millis)} с, потоков: ${threads.size}")
}
