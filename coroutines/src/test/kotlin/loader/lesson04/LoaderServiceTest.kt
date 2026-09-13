package loader.lesson04

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import loader.core.SourceCatalog
import loader.support.captureLog
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoaderServiceTest {

    @Test
    fun `start запускает загрузку каждого файла`() = coTest {
        val catalog = SourceCatalog()
        val service = LoaderService(catalog, StandardTestDispatcher(testScheduler))
        try {
            service.start(listOf(catalog.file("a.csv"), catalog.file("b.csv")))
            advanceTimeBy(1)
            assertEquals(listOf(1, 1), listOf("alpha", "beta").map { catalog.source(it).stats.started })
        } finally {
            service.stop()
        }
    }

    @Test
    fun `после stop новых загрузок нет`() = coTest {
        val catalog = SourceCatalog()
        val service = LoaderService(catalog, StandardTestDispatcher(testScheduler))
        var startedAtStop = 0
        val records = captureLog {
            service.start(listOf(catalog.file("a.csv"), catalog.file("b.csv")))
            advanceTimeBy(2500)
            service.stop()
            startedAtStop = catalog.stats.started
            advanceTimeBy(10_000)
        }
        assertEquals(startedAtStop, catalog.stats.started, "загрузок начато к stop и через 10 с после")
        val afterStop = records.dropWhile { it.message != "stop" }.drop(1)
        assertEquals(emptyList(), afterStop.map { it.message }.filter { "начата загрузка" in it })
    }

    @Test
    fun `stop прерывает идущую загрузку`() = coTest {
        val catalog = SourceCatalog()
        val service = LoaderService(catalog, StandardTestDispatcher(testScheduler))
        service.start(listOf(catalog.file("a.csv")))
        advanceTimeBy(200)
        service.stop()
        runCurrent()
        assertTrue(catalog.source("alpha").stats.cancelled >= 1, "идущая загрузка a.csv не отменена")
    }

    @Test
    fun `остановка одного сервиса не трогает другой`() = coTest {
        val first = SourceCatalog()
        val second = SourceCatalog()
        val one = LoaderService(first, StandardTestDispatcher(testScheduler))
        val two = LoaderService(second, StandardTestDispatcher(testScheduler))
        try {
            one.start(listOf(first.file("a.csv")))
            two.start(listOf(second.file("a.csv")))
            advanceTimeBy(500)
            one.stop()
            advanceTimeBy(3000)
            assertEquals(1, first.stats.started, "загрузок первого сервиса")
            assertEquals(3, second.stats.started, "загрузок второго сервиса за 3,5 с")
        } finally {
            two.stop()
        }
    }

    @Test
    fun `start после stop отклоняется`() = coTest {
        val catalog = SourceCatalog()
        val service = LoaderService(catalog, StandardTestDispatcher(testScheduler))
        service.start(listOf(catalog.file("a.csv")))
        service.stop()
        assertFailsWith<IllegalStateException> { service.start(listOf(catalog.file("a.csv"))) }
    }
}
