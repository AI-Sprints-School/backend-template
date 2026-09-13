package loader.lesson10

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.withTimeout
import loader.core.SourceCatalog
import loader.core.SourceUnavailableException
import loader.support.coTest
import loader.support.namedDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsolatedBatchTest {

    private fun SourceCatalog.files(vararg names: String) = names.map(::file)

    @Test
    fun `сбой одного файла не отменяет соседей`() = coTest {
        val catalog = SourceCatalog()
        val outcome = downloadIsolated(catalog.files("a.csv", "d.csv", "broken.csv", "c.csv"), catalog)
        assertEquals(listOf("a.csv", "d.csv", "c.csv"), outcome.succeeded.map { it.file.name })
    }

    @Test
    fun `нестабильный источник проходит со второй попытки`() = coTest {
        val catalog = SourceCatalog()
        downloadIsolated(catalog.files("d.csv"), catalog)
        assertEquals(2, catalog.source("delta").stats.attempts)
    }

    @Test
    fun `постоянный сбой — три попытки и запись в failed`() = coTest {
        val catalog = SourceCatalog()
        val outcome = downloadIsolated(catalog.files("a.csv", "broken.csv"), catalog)
        assertEquals(3, catalog.source("sigma").stats.attempts, "попыток sigma")
        assertEquals(listOf("broken.csv"), outcome.failed.keys.toList())
        assertTrue(outcome.failed.getValue("broken.csv") is SourceUnavailableException, "в failed не исключение источника")
    }

    @Test
    fun `пауза между попытками растёт`() = coTest {
        val catalog = SourceCatalog()
        downloadIsolated(catalog.files("broken.csv"), catalog)
        assertEquals(2100L, currentTime)
    }

    @Test
    fun `отмена во время паузы не повторяется`() = coTest {
        val catalog = SourceCatalog()
        val job = launch { downloadIsolated(catalog.files("broken.csv"), catalog) }
        advanceTimeBy(600)
        job.cancel()
        job.join()
        advanceUntilIdle()
        val sigma = catalog.source("sigma").stats
        assertEquals(1, sigma.attempts, "попыток sigma после отмены на 600 мс")
        assertTrue(job.isCancelled, "пакет не отменён")
    }

    @Test
    fun `отмена родителя отменяет весь пакет`() = coTest {
        val catalog = SourceCatalog()
        var outcome: BatchOutcome? = null
        val job = launch { outcome = downloadIsolated(catalog.files("a.csv", "c.csv"), catalog) }
        advanceTimeBy(200)
        job.cancel()
        job.join()
        advanceUntilIdle()
        assertNull(outcome, "после отмены пакет вернул итог")
        assertEquals(listOf(1, 1), listOf("alpha", "gamma").map { catalog.source(it).stats.cancelled }, "отменённых загрузок alpha и gamma")
    }
}

class SupervisorContextTest {

    @Test
    fun `загрузка идёт на переданном диспетчере`() {
        val io = namedDispatcher("test-io")
        try {
            val catalog = SourceCatalog(timeScale = 0.01)
            runBlocking(io) {
                withTimeout(30_000) { downloadIsolated(listOf(catalog.file("a.csv"), catalog.file("d.csv")), catalog) }
            }
            val threads = listOf("alpha", "delta").flatMap { catalog.source(it).stats.threadNames }.toSet()
            assertEquals(setOf("test-io"), threads)
        } finally {
            io.close()
        }
    }
}
