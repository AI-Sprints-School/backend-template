package loader.lesson09

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import loader.core.FakeDisk
import loader.core.Log
import loader.core.SourceCatalog
import loader.core.lessonMain
import loader.core.seconds
import kotlin.system.measureTimeMillis

// Сценарий урока 9 — готов, не правится. Запуск: gradle :coroutines:runLesson -Plesson=09

fun main() {
    // агент DebugProbes подгружается до запуска отсчёта времени лога
    DebugProbes.install()
    lessonMain { scenario() }
}

private fun scenario() {
    runBlocking {
        Log.plain("LeakyService: start, 1,2 с, stop, 1,2 с")
        val catalog = SourceCatalog()
        val service = LeakyService(catalog)
        service.start()
        delay(1200)
        service.stop()
        delay(1200)
        dumpNamedCoroutines()

        Log.plain("checksumLoop: отмена через 50 мс")
        val payload = catalog.source("alpha").download(catalog.file("a.csv"))
        val loop = launch(Dispatchers.Default) { checksumLoop(payload, Int.MAX_VALUE) }
        delay(50)
        val stopMillis = measureTimeMillis { loop.cancelAndJoin() }
        Log.plain("checksumLoop: остановлен, отменён = ${loop.isCancelled}, ожидание после cancel < 0,5 с: ${stopMillis < 500}")

        Log.plain("downloadToTemp(slow.bin): отмена через 1 с")
        val disk = FakeDisk()
        val download = launch { downloadToTemp(catalog.file("slow.bin"), catalog, disk) }
        delay(1000)
        download.cancelAndJoin()
        Log.plain("временные файлы после отмены: ${disk.tempFiles}")

        Log.plain("downloadWithTimeout(slow.bin, 2000 мс)")
        var result: Any? = null
        val millis = measureTimeMillis { result = downloadWithTimeout(catalog.file("slow.bin"), catalog) }
        Log.plain("результат: $result за ${seconds(millis)} с")
    }
}

/** Дамп корутин в лог: только корутины с CoroutineName и строка кода урока, где они приостановлены. */
private fun dumpNamedCoroutines() {
    Log.plain("дамп корутин с именем:")
    val named = DebugProbes.dumpCoroutinesInfo().filter { it.context[CoroutineName] != null }
    if (named.isEmpty()) Log.plain("  нет")
    named.forEach { info ->
        val frame = info.lastObservedStackTrace().firstOrNull { it.className.startsWith("loader.lesson09") }
        val place = frame?.let { "${it.fileName}:${it.lineNumber}" } ?: "—"
        Log.plain("  ${info.context[CoroutineName]?.name} ${info.state} $place")
    }
}
