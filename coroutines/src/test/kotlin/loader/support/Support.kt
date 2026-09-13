package loader.support

// Общие помощники тестов курса. Готовая часть шаблона — править не нужно.

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import loader.core.Log
import loader.core.LogRecord
import loader.core.SourceCatalog
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

/** Однопоточный диспетчер с потоком [name]: `test-io`, `test-cpu`. */
fun namedDispatcher(name: String): ExecutorCoroutineDispatcher =
    Executors.newSingleThreadExecutor { task -> Thread(task, name).apply { isDaemon = true } }.asCoroutineDispatcher()

/** Реальное время блока в мс. */
inline fun wallClock(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return (System.nanoTime() - start) / 1_000_000
}

/** DebugProbes.install() / uninstall() вокруг блока. */
inline fun <T> withDebugProbes(block: () -> T): T {
    DebugProbes.install()
    try {
        return block()
    } finally {
        DebugProbes.uninstall()
    }
}

/** runTest курса: таймаут 30 секунд. */
fun coTest(block: suspend TestScope.() -> Unit): TestResult = runTest(timeout = 30.seconds) { block() }

/** Строки лога за время блока; время — виртуальное время теста. */
suspend fun TestScope.captureLog(block: suspend () -> Unit): List<LogRecord> =
    Log.capture({ testScheduler.currentTime }, block)

/** Сумма счётчика по источникам файлов. */
fun SourceCatalog.sumOf(ids: List<String>, counter: (loader.core.SourceStats) -> Int): Int =
    ids.sumOf { counter(source(it).stats) }
