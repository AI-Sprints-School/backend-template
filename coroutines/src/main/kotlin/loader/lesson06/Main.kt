package loader.lesson06

import kotlinx.coroutines.runBlocking
import loader.core.FakeDisk
import loader.core.Log
import loader.core.RemoteFile
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 6 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=06

private fun tenFiles(): List<RemoteFile> {
    val sources = listOf("alpha", "beta", "epsilon")
    return (1..10).map { RemoteFile("f%02d.csv".format(it), sources[(it - 1) % sources.size]) }
}

fun main() = lessonMain {
    runBlocking {
        Log.plain("прогон 1: три файла, Dispatchers.IO и Default, лог шагов")
        val catalog = SourceCatalog(quiet = true)
        val files = listOf("a.csv", "b.csv", "e.csv").map(catalog::file)
        val stored = downloadAndStore(files, catalog, FakeDisk(), LoaderDispatchers())
        Log.plain("записано: ${stored.joinToString { it.name }}")

        Log.plain("прогон 2: десять файлов, Semaphore(3), лог скрыт")
        val semaphoreCatalog = SourceCatalog(quiet = true)
        var millis = measureTimeMillis {
            Log.muted { downloadAndStore(tenFiles(), semaphoreCatalog, FakeDisk(), LoaderDispatchers()) }
        }
        Log.plain("Semaphore(3): ${seconds(millis)} с, maxInFlight = ${semaphoreCatalog.stats.maxInFlight}")

        Log.plain("прогон 3: десять файлов, IO.limitedParallelism(3)")
        val limitedCatalog = SourceCatalog(quiet = true)
        millis = measureTimeMillis { downloadWithLimitedParallelism(tenFiles(), limitedCatalog) }
        Log.plain("limitedParallelism(3): ${seconds(millis)} с, maxInFlight = ${limitedCatalog.stats.maxInFlight}")
    }
}
