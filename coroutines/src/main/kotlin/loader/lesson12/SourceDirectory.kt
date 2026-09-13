package loader.lesson12

import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

// Готово в шаблоне, не правится.

/** Справочник источников: описание по id за delay(100); считает одновременные запросы. */
class SourceDirectory {
    private val inFlight = AtomicInteger()
    private val max = AtomicInteger()

    suspend fun describe(sourceId: String): String {
        max.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
        try {
            delay(100)
        } finally {
            inFlight.decrementAndGet()
        }
        return "источник $sourceId"
    }

    /** Наибольшее число одновременных запросов describe. */
    val maxInFlight: Int get() = max.get()
}

/** Строки журнала coroutines/data/service.log (путь — от каталога модуля). */
fun serviceLogLines(): List<String> = File("data/service.log").readLines()
