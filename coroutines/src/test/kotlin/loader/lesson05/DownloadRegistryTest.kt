package loader.lesson05

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import loader.core.SourceCatalog
import loader.support.coTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private suspend fun TestScope.threeWithCancelledC(catalog: SourceCatalog): DownloadRegistry {
    val registry = DownloadRegistry(this, catalog)
    registry.start("b", catalog.file("b.csv"))
    registry.start("c", catalog.file("c.csv"))
    registry.start("e", catalog.file("e.csv"))
    advanceTimeBy(300)
    registry.cancel("c")
    registry.awaitAll()
    return registry
}

class DownloadRegistryTest {

    @Test
    fun `отмена одной загрузки не трогает остальные`() = coTest {
        val registry = threeWithCancelledC(SourceCatalog())
        val states = registry.states()
        assertEquals(DownloadState.COMPLETED to DownloadState.COMPLETED, states["b"] to states["e"])
    }

    @Test
    fun `отменённая загрузка в состоянии CANCELLED`() = coTest {
        val catalog = SourceCatalog()
        val registry = threeWithCancelledC(catalog)
        assertEquals(DownloadState.CANCELLED, registry.states()["c"])
        assertEquals(1, catalog.source("gamma").stats.cancelled, "отменённых загрузок gamma")
    }

    @Test
    fun `awaitAll ждёт все неотменённые`() = coTest {
        threeWithCancelledC(SourceCatalog())
        assertEquals(800L, currentTime)
    }

    @Test
    fun `отмена неизвестной загрузки возвращает false`() = coTest {
        val registry = DownloadRegistry(this, SourceCatalog())
        assertFalse(registry.cancel("нет"))
    }

    @Test
    fun `отмена завершённой не меняет состояние`() = coTest {
        val catalog = SourceCatalog()
        val registry = DownloadRegistry(this, catalog)
        registry.start("b", catalog.file("b.csv"))
        registry.awaitAll()
        assertFalse(registry.cancel("b"), "cancel завершённой загрузки")
        assertEquals(DownloadState.COMPLETED, registry.states()["b"])
    }
}
