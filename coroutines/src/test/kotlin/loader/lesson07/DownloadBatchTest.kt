package loader.lesson07

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.withTimeout
import loader.core.SourceCatalog
import loader.core.SourceUnavailableException
import loader.support.coTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadBatchTest {

    private fun SourceCatalog.files(vararg names: String) = names.map(::file)

    @Test
    fun `без сбоев вызывающий получает все результаты`() = coTest {
        val catalog = SourceCatalog()
        val result = downloadBatch(catalog.files("b.csv", "c.csv"), catalog, AtomicInteger())
        assertEquals(listOf("b.csv", "c.csv"), result.map { it.file.name })
        assertEquals(1000L, currentTime)
    }

    @Test
    fun `сбой одной загрузки отменяет пакет`() = coTest {
        val catalog = SourceCatalog()
        runCatching { downloadBatch(catalog.files("b.csv", "broken.csv", "c.csv"), catalog, AtomicInteger()) }
        assertEquals(listOf(1, 1), listOf("beta", "gamma").map { catalog.source(it).stats.cancelled }, "отменённых загрузок beta и gamma")
    }

    @Test
    fun `вызывающий получает исключение источника`() = coTest {
        val catalog = SourceCatalog()
        assertFailsWith<SourceUnavailableException> {
            downloadBatch(catalog.files("b.csv", "broken.csv", "c.csv"), catalog, AtomicInteger())
        }
    }

    @Test
    fun `пакет со сбоем заканчивается в момент сбоя`() = coTest {
        val catalog = SourceCatalog()
        runCatching { downloadBatch(catalog.files("b.csv", "broken.csv", "c.csv"), catalog, AtomicInteger()) }
        assertEquals(500L, currentTime)
    }

    @Test
    fun `счётчик готовых точен при параллельной работе`() {
        val catalog = SourceCatalog(timeScale = 0.0, quiet = true)
        val completed = AtomicInteger()
        runBlocking(Dispatchers.Default) {
            withTimeout(30_000) { downloadBatch(catalog.manyFiles(100), catalog, completed) }
        }
        assertEquals(100, completed.get())
    }
}
