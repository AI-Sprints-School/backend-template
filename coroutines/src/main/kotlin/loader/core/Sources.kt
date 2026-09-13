package loader.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToLong
import kotlin.random.Random

/** Внешний источник файлов. Все задержки — уже с учётом `timeScale` каталога. */
interface FileSource {
    val id: String
    val latencyMs: Long

    /** Ждёт через `delay(latencyMs)` — приостанавливает корутину. */
    suspend fun download(file: RemoteFile): Payload

    /** Ждёт через `Thread.sleep(latencyMs)` — блокирует поток. */
    fun downloadBlocking(file: RemoteFile): Payload

    /** Часть файла: `delay(latencyMs / total)`, возвращает процент готовности. */
    suspend fun downloadChunk(file: RemoteFile, index: Int, total: Int): Int

    val stats: SourceStats
}

/** Счётчики источника. Потокобезопасны. */
class SourceStats {
    private val startedCount = AtomicInteger()
    private val completedCount = AtomicInteger()
    private val cancelledCount = AtomicInteger()
    private val failedCount = AtomicInteger()
    private val inFlightCount = AtomicInteger()
    private val maxInFlightCount = AtomicInteger()
    private val attemptsCount = AtomicInteger()
    private val threads = ConcurrentHashMap.newKeySet<String>()

    val started: Int get() = startedCount.get()
    val completed: Int get() = completedCount.get()
    val cancelled: Int get() = cancelledCount.get()
    val failed: Int get() = failedCount.get()
    val inFlight: Int get() = inFlightCount.get()
    val maxInFlight: Int get() = maxInFlightCount.get()

    /** Вызовов download и downloadBlocking. */
    val attempts: Int get() = attemptsCount.get()

    /** Имена потоков без суффикса отладки. */
    val threadNames: Set<String> get() = threads.toSet()

    fun reset() {
        listOf(startedCount, completedCount, cancelledCount, failedCount, inFlightCount, maxInFlightCount, attemptsCount)
            .forEach { it.set(0) }
        threads.clear()
    }

    internal fun onAttempt(): Int = attemptsCount.incrementAndGet()
    internal fun onStart() {
        startedCount.incrementAndGet()
        val now = inFlightCount.incrementAndGet()
        maxInFlightCount.accumulateAndGet(now, ::maxOf)
    }
    internal fun onLeave() { inFlightCount.decrementAndGet() }
    internal fun onCompleted() { completedCount.incrementAndGet() }
    internal fun onCancelled() { cancelledCount.incrementAndGet() }
    internal fun onFailed() { failedCount.incrementAndGet() }
    internal fun onThread() {
        // имя потока запоминается один раз на смену потока: десять тысяч загрузок урока 1 не тратят время на строки
        val name = Thread.currentThread().name
        if (name == lastThreadName) return
        lastThreadName = name
        threads += plainThreadName(name)
    }

    @Volatile private var lastThreadName: String? = null
}

private enum class Behavior { RELIABLE, FLAKY_FIRST, ALWAYS_FAILS }

private data class SourceSpec(val id: String, val seed42LatencyMs: Long, val behavior: Behavior)

// Порядок — как в таблице LOADER_SPEC §3.2: от него зависят задержки при другом seed.
private val SPECS = listOf(
    SourceSpec("alpha", 400, Behavior.RELIABLE),
    SourceSpec("beta", 700, Behavior.RELIABLE),
    SourceSpec("gamma", 1000, Behavior.RELIABLE),
    SourceSpec("epsilon", 800, Behavior.RELIABLE),
    SourceSpec("omega", 3000, Behavior.RELIABLE),
    SourceSpec("delta", 600, Behavior.FLAKY_FIRST),
    SourceSpec("sigma", 500, Behavior.ALWAYS_FAILS),
)

private val FILES = listOf(
    RemoteFile("a.csv", "alpha"),
    RemoteFile("b.csv", "beta"),
    RemoteFile("c.csv", "gamma"),
    RemoteFile("d.csv", "delta"),
    RemoteFile("e.csv", "epsilon"),
    RemoteFile("slow.bin", "omega"),
    RemoteFile("broken.csv", "sigma"),
)

/**
 * Каталог имитированных источников. Всё детерминировано по [seed]:
 * при seed 42 задержки — как в уроках (alpha 0,4 с, beta 0,7 с, gamma 1,0 с …).
 * [timeScale] умножает все задержки; [quiet] — источники не пишут в лог.
 */
class SourceCatalog(val seed: Long = 42, val timeScale: Double = 1.0, val quiet: Boolean = false) {

    /** Счётчики всех источников каталога вместе. */
    val stats = SourceStats()

    val sources: List<FileSource> = SPECS.mapIndexed { index, spec ->
        val base = when {
            spec.id == "omega" -> 3000L
            seed == 42L -> spec.seed42LatencyMs
            else -> 100L * (2 + Random(seed * 31 + index).nextInt(0, 19))
        }
        SimulatedSource(spec.id, (base * timeScale).roundToLong(), spec.behavior, quiet, stats)
    }

    private val byId = sources.associateBy { it.id }

    fun source(id: String): FileSource = byId[id] ?: error("нет источника $id")

    fun file(name: String): RemoteFile = FILES.firstOrNull { it.name == name } ?: error("нет файла $name")

    fun firstFileOf(sourceId: String): RemoteFile =
        FILES.firstOrNull { it.sourceId == sourceId } ?: error("у источника $sourceId нет файлов")

    /** [count] файлов одного источника: `alpha-00001.csv`, `alpha-00002.csv` … */
    fun manyFiles(count: Int, sourceId: String = "alpha"): List<RemoteFile> {
        source(sourceId)
        return (1..count).map { RemoteFile("$sourceId-${it.toString().padStart(5, '0')}.csv", sourceId) }
    }
}

private class SimulatedSource(
    override val id: String,
    override val latencyMs: Long,
    private val behavior: Behavior,
    private val quiet: Boolean,
    private val total: SourceStats,
) : FileSource {

    override val stats = SourceStats()

    private fun say(message: String) {
        if (!quiet) log(message)
    }

    private fun payload(file: RemoteFile) =
        Payload(file, sizeBytes = (1000 + latencyMs).toInt(), bytesSeed = file.name.hashCode().toLong() * 31 + latencyMs)

    private fun both(action: SourceStats.() -> Unit) {
        stats.action()
        total.action()
    }

    /** Номер попытки или сбой после задержки. */
    private fun finish(file: RemoteFile, attempt: Int): Payload {
        val fails = when (behavior) {
            Behavior.RELIABLE -> false
            Behavior.FLAKY_FIRST -> attempt == 1
            Behavior.ALWAYS_FAILS -> true
        }
        both { onThread() }
        if (fails) {
            both { onFailed() }
            say("$id: сбой ${file.name} — попытка $attempt")
            throw SourceUnavailableException(id, attempt)
        }
        both { onCompleted() }
        val result = payload(file)
        say("$id: готово ${file.name} (${result.sizeBytes} байт)")
        return result
    }

    private fun begin(file: RemoteFile): Int {
        val attempt = stats.onAttempt()
        total.onAttempt()
        both { onStart(); onThread() }
        say("$id: начата загрузка ${file.name}")
        return attempt
    }

    override suspend fun download(file: RemoteFile): Payload {
        val attempt = begin(file)
        try {
            delay(latencyMs)
        } catch (e: CancellationException) {
            both { onCancelled() }
            say("$id: отменена загрузка ${file.name}")
            throw e
        } finally {
            both { onLeave() }
        }
        return finish(file, attempt)
    }

    override fun downloadBlocking(file: RemoteFile): Payload {
        val attempt = begin(file)
        try {
            Thread.sleep(latencyMs)
        } finally {
            both { onLeave() }
        }
        return finish(file, attempt)
    }

    override suspend fun downloadChunk(file: RemoteFile, index: Int, total: Int): Int {
        require(total > 0 && index in 0 until total) { "часть $index из $total" }
        if (index == 0) {
            both { onStart(); onThread() }
            say("$id: начата загрузка ${file.name}")
        }
        try {
            delay(latencyMs / total)
        } catch (e: CancellationException) {
            both { onCancelled(); onLeave() }
            say("$id: отменена загрузка ${file.name}")
            throw e
        }
        val percent = (index + 1) * 100 / total
        if (index == total - 1) {
            both { onLeave(); onCompleted(); onThread() }
            say("$id: готово ${file.name} (${payload(file).sizeBytes} байт)")
        }
        return percent
    }
}
