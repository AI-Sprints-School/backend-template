package loader.lesson08

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.withTimeout
import loader.core.Log
import loader.core.SourceCatalog
import loader.support.coTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FailurePolicyTest {

    @Test
    fun `сбой в launch доходит до обработчика корня один раз`() = runBlocking {
        val records = Log.capture {
            val scope = serviceScope(rootHandler())
            withTimeout(5_000) { scope.launch { throw IOException("диск полон") }.join() }
            scope.cancel()
        }
        assertEquals(listOf("сбой задачи: диск полон"), records.map { it.message }.filter { it.startsWith("сбой задачи:") })
    }

    @Test
    fun `исключение, пойманное у await, обработчик не видит`() = runBlocking {
        val records = Log.capture {
            val scope = serviceScope(rootHandler())
            val deferred = scope.async { throw IOException("диск полон") }
            val caught = runCatching { withTimeout(5_000) { deferred.await() } }.exceptionOrNull()
            assertTrue(caught is IOException, "у await поймано не исключение задачи")
            delay(100)
            scope.cancel()
        }
        assertEquals(emptyList(), records.map { it.message }.filter { it.startsWith("сбой задачи:") })
    }

    @Test
    fun `loadOrNull возвращает null при сбое источника`() = coTest {
        val catalog = SourceCatalog()
        assertNull(loadOrNull(catalog.file("broken.csv"), catalog))
    }

    @Test
    fun `loadOrNull возвращает данные надёжного источника`() = coTest {
        val catalog = SourceCatalog()
        assertEquals("a.csv", assertNotNull(loadOrNull(catalog.file("a.csv"), catalog)).file.name)
    }

    @Test
    fun `loadOrNull не глотает отмену`() = coTest {
        val catalog = SourceCatalog()
        var returned = false
        var failure: Throwable? = null
        val job = launch {
            try {
                loadOrNull(catalog.file("c.csv"), catalog)
                returned = true
            } catch (e: Throwable) {
                failure = e
                throw e
            }
        }
        advanceTimeBy(100)
        job.cancel()
        job.join()
        assertTrue(failure is CancellationException, "loadOrNull завершился не отменой: ${failure?.let { it::class.simpleName }}")
        assertFalse(returned, "после отмены loadOrNull вернул значение")
    }
}
