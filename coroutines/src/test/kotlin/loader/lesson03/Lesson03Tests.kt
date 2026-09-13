package loader.lesson03

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import loader.core.AuthException
import loader.core.RequestKind
import loader.core.ScheduledCallbackClient
import loader.core.SourceCatalog
import loader.support.coTest
import loader.support.wallClock
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Клиент в виртуальном времени теста; сбои колбэков собираются в [errors]. */
private class ClientFixture(test: TestScope, ignoreCancel: Boolean = false) {
    val errors: MutableList<Throwable> = Collections.synchronizedList(mutableListOf())
    val catalog = SourceCatalog()
    private val scope = CoroutineScope(
        StandardTestDispatcher(test.testScheduler) + SupervisorJob() + CoroutineExceptionHandler { _, e -> errors += e },
    )
    val client = ScheduledCallbackClient(scope, catalog, ignoreCancel)
}

class SuspendCallTest {

    @Test
    fun `вызов возвращает результат колбэка`() = coTest {
        val f = ClientFixture(this)
        assertEquals("token-student", call(f.client, RequestKind.AUTH, "student"))
    }

    @Test
    fun `ошибка колбэка выбрасывается исключением`() = coTest {
        val f = ClientFixture(this)
        assertFailsWith<AuthException> { call(f.client, RequestKind.AUTH, "blocked") }
    }

    @Test
    fun `отмена вызывающего отменяет запрос клиента`() = coTest {
        val f = ClientFixture(this)
        val job = launch { call(f.client, RequestKind.AUTH, "student") }
        advanceTimeBy(100)
        job.cancel()
        job.join()
        assertEquals(listOf("AUTH:student"), f.client.cancelledRequests)
    }

    @Test
    fun `поздний колбэк после отмены не ломает вызов`() = coTest {
        val f = ClientFixture(this, ignoreCancel = true)
        var failure: Throwable? = null
        val job = launch {
            try {
                call(f.client, RequestKind.AUTH, "student")
            } catch (e: Throwable) {
                failure = e
                throw e
            }
        }
        advanceTimeBy(100)
        job.cancel()
        advanceUntilIdle()
        assertTrue(job.isCancelled, "корутина вызова не отменена")
        assertTrue(failure == null || failure is kotlinx.coroutines.CancellationException, "вызов завершился не отменой: $failure")
        assertEquals(emptyList<String?>(), f.errors.map { it::class.simpleName }, "исключения в колбэке клиента")
    }
}

class FetchChainTest {

    @Test
    fun `шаги идут по порядку`() = coTest {
        val f = ClientFixture(this)
        fetchFile(f.client, "student", f.catalog.file("c.csv"))
        assertEquals(listOf("AUTH:student", "METADATA:c.csv", "FILE:c.csv"), f.client.requests)
    }

    @Test
    fun `время цепочки — сумма шагов`() = coTest {
        val f = ClientFixture(this)
        fetchFile(f.client, "student", f.catalog.file("c.csv"))
        assertEquals(1500L, currentTime)
    }

    @Test
    fun `две цепочки параллельно занимают время самой долгой`() = coTest {
        val f = ClientFixture(this)
        launch { fetchFile(f.client, "student", f.catalog.file("a.csv")) }
        launch { fetchFile(f.client, "student", f.catalog.file("c.csv")) }
        advanceUntilIdle()
        assertEquals(1500L, currentTime)
    }

    @Test
    fun `цепочка не блокирует поток`() = coTest {
        val f = ClientFixture(this)
        val millis = wallClock { fetchFile(f.client, "student", f.catalog.file("c.csv")) }
        assertTrue(millis < 500, "цепочка заняла 500 мс реального времени или больше — поток ждал, а не приостанавливался")
    }
}
