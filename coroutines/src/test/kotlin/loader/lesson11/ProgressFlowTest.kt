package loader.lesson11

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withTimeout
import loader.core.FakeDisk
import loader.core.SourceCatalog
import loader.core.plainThreadName
import loader.support.coTest
import loader.support.namedDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressFlowTest {

    @Test
    fun `без collect загрузка не начинается`() = coTest {
        val catalog = SourceCatalog()
        progressFlow(catalog.file("c.csv"), catalog)
        advanceTimeBy(5000)
        assertEquals(0, catalog.source("gamma").stats.started)
    }

    @Test
    fun `повторный collect запускает загрузку заново`() = coTest {
        val catalog = SourceCatalog()
        val flow = progressFlow(catalog.file("c.csv"), catalog)
        flow.collect()
        flow.collect()
        assertEquals(2, catalog.source("gamma").stats.started)
    }

    @Test
    fun `прогресс идёт от 20 до 100`() = coTest {
        val catalog = SourceCatalog()
        assertEquals(listOf(20, 40, 60, 80, 100), progressFlow(catalog.file("c.csv"), catalog).toList().map { it.percent })
    }

    @Test
    fun `чтение с диска идёт на диспетчере ввода-вывода`() {
        val io = namedDispatcher("test-io")
        try {
            val disk = FakeDisk(ioDelayMs = 5)
            disk.putLines("service.log", listOf("a", "b", "c"))
            val lines = runBlocking { withTimeout(30_000) { storedLines(disk, "service.log", io).toList() } }
            assertEquals(listOf("a", "b", "c"), lines)
            assertEquals(listOf("test-io"), disk.readThreads)
        } finally {
            io.close()
        }
    }

    @Test
    fun `collect получает строки в своём контексте`() {
        val io = namedDispatcher("test-io")
        try {
            val disk = FakeDisk(ioDelayMs = 5)
            disk.putLines("service.log", listOf("a", "b", "c"))
            val threads = mutableListOf<String>()
            runBlocking {
                withTimeout(30_000) { storedLines(disk, "service.log", io).collect { threads += plainThreadName() } }
            }
            assertEquals(List(3) { plainThreadName() }, threads)
        } finally {
            io.close()
        }
    }
}
