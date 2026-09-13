package loader.lesson06

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import loader.core.Checksum
import loader.core.FakeDisk
import loader.core.Log
import loader.core.SourceCatalog
import loader.support.namedDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoaderDispatchersTest {

    private val io = namedDispatcher("test-io")
    private val cpu = namedDispatcher("test-cpu")
    private val dispatchers = LoaderDispatchers(io, cpu)

    @AfterTest
    fun closeDispatchers() {
        io.close()
        cpu.close()
    }

    private fun run(block: suspend () -> Unit) = runBlocking { withTimeout(30_000) { block() } }

    @Test
    fun `запись на диск идёт на диспетчере ввода-вывода`() = run {
        val catalog = SourceCatalog(timeScale = 0.01)
        val disk = FakeDisk(ioDelayMs = 5)
        downloadAndStore(catalog.manyFiles(5), catalog, disk, dispatchers)
        assertEquals(List(5) { "test-io" }, disk.writeThreads)
    }

    @Test
    fun `контрольная сумма считается на диспетчере вычислений`() = run {
        val catalog = SourceCatalog(timeScale = 0.01)
        Checksum.reset()
        downloadAndStore(catalog.manyFiles(3), catalog, FakeDisk(ioDelayMs = 5), dispatchers)
        assertEquals(setOf("test-cpu"), Checksum.callThreads)
    }

    @Test
    fun `одновременно не больше трёх загрузок`() = run {
        val catalog = SourceCatalog(timeScale = 0.01)
        downloadAndStore(catalog.manyFiles(10), catalog, FakeDisk(ioDelayMs = 5), dispatchers)
        val max = catalog.source("alpha").stats.maxInFlight
        assertTrue(max <= 3, "одновременных загрузок было $max, а не больше трёх")
    }

    @Test
    fun `ограничение не делает загрузку последовательной`() = run {
        val catalog = SourceCatalog(timeScale = 0.01)
        val stored = downloadAndStore(catalog.manyFiles(10), catalog, FakeDisk(ioDelayMs = 5), dispatchers)
        assertEquals(10, stored.size, "записанных файлов")
        assertEquals(3, catalog.source("alpha").stats.maxInFlight, "одновременных загрузок")
    }

    @Test
    fun `имя корутины загрузки видно в логе`() = run {
        val catalog = SourceCatalog(timeScale = 0.01)
        val records = Log.capture {
            downloadAndStore(listOf(catalog.file("a.csv"), catalog.file("b.csv")), catalog, FakeDisk(ioDelayMs = 5), dispatchers)
        }
        assertTrue(records.any { "download-a.csv" in it.thread }, "в именах потоков лога нет корутины download-a.csv")
    }
}
