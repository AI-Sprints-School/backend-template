package loader.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong

/** Вид запроса к клиенту источника и время ответа. У FILE — задержка источника файла. */
enum class RequestKind(val latencyMs: Long) { AUTH(200), METADATA(300), FILE(0) }

/** Ручка отправленного запроса: отменённый запрос колбэк не вызывает. */
fun interface RequestHandle {
    fun cancel()
}

/** Клиент источника со старым API на колбэках (урок 3). */
interface CallbackSourceClient {
    /** Отправляет запрос и сразу возвращается; ответ приходит колбэком на потоке клиента. */
    fun request(
        kind: RequestKind,
        arg: String,
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ): RequestHandle

    /** Блокирующий вызов: ждёт ответа на потоке вызывающего и там же пишет лог. */
    fun requestBlocking(kind: RequestKind, arg: String): String

    /** Отправленные запросы по порядку: "AUTH:student", "METADATA:c.csv", "FILE:c.csv". */
    val requests: List<String>

    /** Запросы, у которых вызван cancel(). */
    val cancelledRequests: List<String>
}

/** Общая часть двух реализаций: время ответа, результат, учёт запросов. */
abstract class BaseCallbackClient(protected val catalog: SourceCatalog) : CallbackSourceClient {
    private val sent = Collections.synchronizedList(mutableListOf<String>())
    private val cancelled = Collections.synchronizedList(mutableListOf<String>())

    override val requests: List<String> get() = synchronized(sent) { sent.toList() }
    override val cancelledRequests: List<String> get() = synchronized(cancelled) { cancelled.toList() }

    protected fun latencyOf(kind: RequestKind, arg: String): Long = when (kind) {
        RequestKind.FILE -> catalog.source(catalog.file(arg).sourceId).latencyMs
        else -> (kind.latencyMs * catalog.timeScale).roundToLong()
    }

    /** Результат запроса или исключение источника. */
    protected fun answer(kind: RequestKind, arg: String): Result<String> = when (kind) {
        RequestKind.AUTH ->
            if (arg == "blocked") Result.failure(AuthException(arg)) else Result.success("token-$arg")
        RequestKind.METADATA -> {
            val file = catalog.file(arg)
            Result.success("meta:$arg:${1000 + catalog.source(file.sourceId).latencyMs}")
        }
        RequestKind.FILE -> Result.success("content:$arg")
    }

    protected fun register(kind: RequestKind, arg: String): String = "$kind:$arg".also { sent += it }

    protected fun markCancelled(key: String) {
        cancelled += key
    }

    override fun requestBlocking(kind: RequestKind, arg: String): String {
        register(kind, arg)
        Thread.sleep(latencyOf(kind, arg))
        log("клиент: ответ $kind $arg")
        return answer(kind, arg).getOrThrow()
    }

    protected fun deliver(kind: RequestKind, arg: String, onResult: (String) -> Unit, onError: (Throwable) -> Unit) {
        log("клиент: ответ $kind $arg")
        answer(kind, arg).fold(onResult, onError)
    }
}

/**
 * Клиент для runLesson: запрос выполняется на пуле из двух потоков
 * `source-callback-1`, `source-callback-2`, колбэк вызывается там же.
 */
class ThreadedCallbackClient(catalog: SourceCatalog) : BaseCallbackClient(catalog) {
    private val pool = Executors.newFixedThreadPool(2, object : ThreadFactory {
        private val number = AtomicInteger()
        override fun newThread(task: Runnable) =
            Thread(task, "source-callback-${number.incrementAndGet()}").apply { isDaemon = true }
    })

    override fun request(
        kind: RequestKind,
        arg: String,
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ): RequestHandle {
        val key = register(kind, arg)
        val isCancelled = AtomicBoolean(false)
        pool.execute {
            Thread.sleep(latencyOf(kind, arg))
            if (isCancelled.get()) log("клиент: запрос $kind отменён")
            else deliver(kind, arg, onResult, onError)
        }
        return RequestHandle {
            if (isCancelled.compareAndSet(false, true)) markCancelled(key)
        }
    }
}

/**
 * Клиент для тестов: то же через `scope.launch { delay(…) }` — в виртуальном времени runTest.
 * [ignoreCancel] = true — колбэк приходит, даже если запрос отменён.
 */
class ScheduledCallbackClient(
    private val scope: CoroutineScope,
    catalog: SourceCatalog,
    private val ignoreCancel: Boolean = false,
) : BaseCallbackClient(catalog) {

    override fun request(
        kind: RequestKind,
        arg: String,
        onResult: (String) -> Unit,
        onError: (Throwable) -> Unit,
    ): RequestHandle {
        val key = register(kind, arg)
        val isCancelled = AtomicBoolean(false)
        scope.launch {
            delay(latencyOf(kind, arg))
            if (isCancelled.get() && !ignoreCancel) log("клиент: запрос $kind отменён")
            else deliver(kind, arg, onResult, onError)
        }
        return RequestHandle {
            if (isCancelled.compareAndSet(false, true)) markCancelled(key)
        }
    }
}
