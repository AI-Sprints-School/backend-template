package loader.lesson09

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.withTimeout
import loader.core.FakeDisk
import loader.core.Log
import loader.core.SourceCatalog
import loader.support.coTest
import loader.support.wallClock
import loader.support.withDebugProbes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShutdownTest {

    @Test
    fun `цикл контрольной суммы останавливается по отмене`() = runBlocking {
        val catalog = SourceCatalog(timeScale = 0.0, quiet = true)
        val payload = catalog.source("alpha").download(catalog.file("a.csv"))
        var cause: Throwable? = null
        val job = launch(Dispatchers.Default) { checksumLoop(payload, Int.MAX_VALUE) }
        job.invokeOnCompletion { cause = it }
        Thread.sleep(20)
        val millis = wallClock {
            job.cancel()
            withTimeout(10_000) { job.join() }
        }
        assertTrue(cause is CancellationException, "цикл завершился не отменой: ${cause?.let { it::class.simpleName }}")
        assertTrue(millis < 500, "после cancel цикл работал 500 мс или дольше")
    }

    @Test
    fun `временный файл удалён после отмены загрузки`() = coTest {
        val catalog = SourceCatalog()
        val disk = FakeDisk()
        var cause: Throwable? = null
        val job = launch { downloadToTemp(catalog.file("slow.bin"), catalog, disk) }
        job.invokeOnCompletion { cause = it }
        advanceTimeBy(1000)
        job.cancel()
        job.join()
        assertTrue(cause is CancellationException, "загрузка завершилась не отменой: ${cause?.let { it::class.simpleName }}")
        assertEquals(emptySet(), disk.tempFiles)
    }

    @Test
    fun `уборка с приостановкой выполняется после отмены`() = coTest {
        val catalog = SourceCatalog()
        val disk = FakeDisk()
        val job = launch { downloadToTemp(catalog.file("slow.bin"), catalog, disk) }
        advanceTimeBy(1000)
        job.cancel()
        job.join()
        assertEquals(1, disk.deletedTemps.size, "завершённых удалений временного файла")
        assertEquals(1000L + disk.ioDelayMs, currentTime, "удаление с delay шло после отмены")
    }

    @Test
    fun `медленный источник обрывается по таймауту`() = coTest {
        val catalog = SourceCatalog()
        assertNull(downloadWithTimeout(catalog.file("slow.bin"), catalog))
        assertEquals(2000L, currentTime)
    }

    @Test
    fun `быстрый источник укладывается в таймаут`() = coTest {
        val catalog = SourceCatalog()
        assertEquals("a.csv", assertNotNull(downloadWithTimeout(catalog.file("a.csv"), catalog)).file.name)
    }
}

class LeakyServiceTest {

    @Test
    fun `после stop в логе нет строк heartbeat`() = runBlocking {
        val service = LeakyService(SourceCatalog(quiet = true))
        val records = Log.capture {
            service.start()
            delay(600)
            service.stop()
            delay(700)
        }
        val afterStop = records.dropWhile { it.message != "stop" }.drop(1).map { it.message }
        assertEquals(emptyList(), afterStop.filter { it.startsWith("heartbeat") })
    }

    @Test
    fun `после stop корутин сервиса не осталось`() = runBlocking {
        val alive = withDebugProbes {
            val service = LeakyService(SourceCatalog(quiet = true))
            service.start()
            delay(300)
            service.stop()
            delay(200)
            DebugProbes.dumpCoroutinesInfo()
                .filter { it.job?.isActive == true }
                .mapNotNull { it.context[CoroutineName]?.name }
                .filter { it == "heartbeat" || it == "leaky-download" }
        }
        assertEquals(emptyList(), alive)
    }
}
