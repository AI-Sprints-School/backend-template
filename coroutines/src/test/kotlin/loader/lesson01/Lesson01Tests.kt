package loader.lesson01

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.currentTime
import loader.core.SourceCatalog
import loader.support.coTest
import loader.support.wallClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun SourceCatalog.abc() = listOf("a.csv", "b.csv", "c.csv").map(::file)

class SequentialLoadTest {

    @Test
    fun `подряд — время равно сумме задержек`() = coTest {
        val catalog = SourceCatalog()
        loadSequential(catalog.abc(), catalog)
        assertEquals(2100L, currentTime)
    }

    @Test
    fun `подряд — результаты в порядке файлов`() = coTest {
        val catalog = SourceCatalog()
        val result = loadSequential(catalog.abc(), catalog)
        assertEquals(listOf("a.csv", "b.csv", "c.csv"), result.map { it.file.name })
    }

    @Test
    fun `подряд — другой seed`() = coTest {
        val catalog = SourceCatalog(seed = 7)
        val files = catalog.abc()
        loadSequential(files, catalog)
        assertEquals(files.sumOf { catalog.source(it.sourceId).latencyMs }, currentTime)
    }
}

class ConcurrentLoadTest {

    @Test
    fun `параллельно — время равно самой долгой загрузке`() = coTest {
        val catalog = SourceCatalog()
        loadConcurrent(catalog.abc(), catalog)
        assertEquals(1000L, currentTime)
    }

    @Test
    fun `параллельно — каждый файл загружен один раз`() = coTest {
        val catalog = SourceCatalog()
        val files = catalog.abc()
        loadConcurrent(files, catalog)
        assertEquals(listOf(1, 1, 1), files.map { catalog.source(it.sourceId).stats.completed })
    }

    @Test
    fun `параллельно — функция возвращается после всех загрузок`() = coTest {
        val catalog = SourceCatalog()
        val files = catalog.abc()
        loadConcurrent(files, catalog)
        val sources = files.map { catalog.source(it.sourceId).stats }
        assertEquals(3, sources.sumOf { it.completed }, "готовых загрузок сразу после возврата")
        assertEquals(0, sources.sumOf { it.inFlight }, "идущих загрузок сразу после возврата")
    }

    @Test
    fun `параллельно — поток не блокируется`() = coTest {
        val catalog = SourceCatalog()
        val files = catalog.abc()
        val millis = wallClock { loadConcurrent(files, catalog) }
        assertTrue(millis < 500, "вызов занял 500 мс реального времени или больше — поток ждал, а не приостанавливался")
    }
}

class BlockingLoadTest {

    @Test
    fun `с блокировкой — на одном потоке время равно сумме`() {
        val catalog = SourceCatalog(timeScale = 0.1)
        val files = catalog.abc()
        val millis = wallClock { runBlocking { loadConcurrentBlocking(files, catalog) } }
        val threads = files.flatMap { catalog.source(it.sourceId).stats.threadNames }.toSet()
        assertTrue(millis >= 190, "загрузки 40 + 70 + 100 мс на одном потоке заняли меньше 190 мс")
        assertEquals(1, threads.size, "потоков загрузки")
    }
}
