package loader.lesson13

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import loader.core.SourceCatalog
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoaderStateTest {

    @Test
    fun `начальное состояние IDLE`() = coTest {
        val loader = StatefulLoader(backgroundScope, SourceCatalog())
        assertEquals(LoaderSnapshot(Status.IDLE, 0, 0), loader.state.value)
    }

    @Test
    fun `статусы идут по порядку`() = coTest {
        val catalog = SourceCatalog()
        val loader = StatefulLoader(backgroundScope, catalog)
        val statuses = mutableListOf<Status>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            loader.state.collect { if (statuses.lastOrNull() != it.status) statuses += it.status }
        }
        loader.start(listOf(catalog.file("a.csv"), catalog.file("c.csv")))
        advanceTimeBy(100)
        loader.stop()
        runCurrent()
        assertEquals(listOf(Status.IDLE, Status.RUNNING, Status.STOPPING, Status.STOPPED), statuses)
    }

    @Test
    fun `STOPPED наступает после завершения всех загрузок`() = coTest {
        val catalog = SourceCatalog()
        val loader = StatefulLoader(backgroundScope, catalog)
        var inFlightAtStopped = -1
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            loader.state.first { it.status == Status.STOPPED }
            inFlightAtStopped = catalog.sources.sumOf { it.stats.inFlight }
        }
        loader.start(listOf(catalog.file("a.csv"), catalog.file("b.csv"), catalog.file("c.csv")))
        advanceTimeBy(100)
        loader.stop()
        runCurrent()
        assertEquals(0, inFlightAtStopped, "идущих загрузок в момент STOPPED")
    }

    @Test
    fun `start из STOPPING отклоняется`() = coTest {
        val catalog = SourceCatalog()
        // Загрузки идут на отдельном планировщике, который тест не продвигает: stop() застревает в STOPPING.
        val frozen = TestCoroutineScheduler()
        val loaderScope = CoroutineScope(StandardTestDispatcher(frozen) + Job())
        val loader = StatefulLoader(loaderScope, catalog)
        loader.start(listOf(catalog.file("a.csv")))
        frozen.runCurrent()
        val stopping = launch { loader.stop() }
        runCurrent()
        assertEquals(Status.STOPPING, loader.state.value.status, "статус во время остановки")
        assertFailsWith<IllegalStateException> { loader.start(listOf(catalog.file("b.csv"))) }
        frozen.advanceUntilIdle()
        stopping.join()
        loaderScope.cancel()
    }

    @Test
    fun `счётчик готовых точен при параллельных обновлениях`() = runBlocking {
        val catalog = SourceCatalog(timeScale = 0.0, quiet = true)
        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            val loader = StatefulLoader(scope, catalog)
            loader.start(catalog.manyFiles(100))
            val snapshot = runCatching { withTimeout(10_000) { loader.state.first { it.completed + it.failed >= 100 } } }
                .getOrElse { loader.state.value }
            assertEquals(100, snapshot.completed, "готовых загрузок из 100")
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `наблюдатель не видит одинаковых состояний подряд`() = coTest {
        val catalog = SourceCatalog()
        val loader = StatefulLoader(backgroundScope, catalog)
        val seen = mutableListOf<LoaderSnapshot>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { loader.state.collect { seen += it } }
        loader.start(emptyList())
        loader.stop()
        runCurrent()
        assertTrue(seen.isNotEmpty(), "наблюдатель не получил состояний")
        assertEquals(seen.zipWithNext().count { (a, b) -> a == b }, 0, "одинаковых состояний подряд: $seen")
    }
}
