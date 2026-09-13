package loader.core

import java.io.File

/** Строка лога: сообщение, поток и время с запуска (в тестах на runTest — виртуальное). */
data class LogRecord(val message: String, val thread: String, val elapsedMs: Long)

/** Пишет строку в лог загрузчика: время с запуска, поток, сообщение. */
fun log(message: String) = Log.write(message)

/**
 * Время в секундах с одной цифрой после запятой: 2100 → «2,1».
 * Сотые отбрасываются, а не округляются: накладные расходы запуска (десятки миллисекунд)
 * не превращают 2,1 с в 2,2 — числа в логе совпадают с числами уроков.
 */
fun seconds(ms: Long): String {
    val tenths = maxOf(ms, 0) / 100
    return "${tenths / 10},${tenths % 10}"
}

object Log {
    /** Лимит файла run-NN.txt в знаках. */
    const val REPORT_LIMIT = 2000
    private const val CUT_MARK = "… вывод обрезан шаблоном: лимит 2000 знаков"

    private val lock = Any()
    @Volatile private var startNanos = System.nanoTime()
    private val reportFile: File? = System.getProperty("loader.report")?.takeIf { it.isNotBlank() }?.let(::File)
    private var written = 0
    private var cut = false

    @Volatile private var sink: MutableList<LogRecord>? = null
    @Volatile private var clock: (() -> Long)? = null

    /** Начало отсчёта времени и новый файл отчёта. Зовёт [lessonMain]. */
    fun start() = synchronized(lock) {
        startNanos = System.nanoTime()
        written = 0
        cut = false
        reportFile?.let { it.parentFile?.mkdirs(); it.writeText("") }
    }

    fun write(message: String) {
        val thread = Thread.currentThread().name
        val elapsed = clock?.invoke() ?: ((System.nanoTime() - startNanos) / 1_000_000)
        synchronized(lock) {
            val capturing = sink
            if (capturing != null) {
                capturing += LogRecord(message, thread, elapsed)
            } else if (mutedDepth == 0) {
                emit("${seconds(elapsed).padStart(5)} с | $thread | $message")
            }
        }
    }

    /** Строка без времени и потока — заголовки и итоги режимов в Main.kt. */
    fun plain(text: String) = synchronized(lock) {
        val capturing = sink
        if (capturing != null) capturing += LogRecord(text, "", clock?.invoke() ?: 0) else emit(text)
    }

    private fun emit(line: String) {
        println(line)
        val file = reportFile ?: return
        if (cut) return
        if (written + line.length + 1 > REPORT_LIMIT - CUT_MARK.length - 1) {
            file.appendText(CUT_MARK + "\n")
            cut = true
            return
        }
        file.appendText(line + "\n")
        written += line.length + 1
    }

    @Volatile private var mutedDepth = 0

    /** Сценарий Main.kt: строки лога за время [block] не выводятся — чтобы прогон на много файлов уложился в лимит. */
    suspend fun <T> muted(block: suspend () -> T): T {
        synchronized(lock) { mutedDepth++ }
        try {
            return block()
        } finally {
            synchronized(lock) { mutedDepth-- }
        }
    }

    /**
     * Для тестов: собирает строки лога, записанные за время [block], с любых потоков.
     * [clock] — источник времени (в тестах на runTest — виртуальное время планировщика).
     */
    suspend fun capture(clock: (() -> Long)? = null, block: suspend () -> Unit): List<LogRecord> {
        val records = mutableListOf<LogRecord>()
        synchronized(lock) {
            sink = records
            this.clock = clock
        }
        try {
            block()
        } finally {
            synchronized(lock) {
                sink = null
                this.clock = null
            }
        }
        return synchronized(lock) { records.toList() }
    }
}

/**
 * Обёртка функции main урока: запускает отсчёт времени лога и превращает
 * `TODO("Урок NN: …")` в короткое сообщение `NotImplementedError: Урок NN: …`.
 */
fun lessonMain(block: () -> Unit) {
    Log.start()
    try {
        block()
    } catch (e: NotImplementedError) {
        val short = NotImplementedError(e.message.orEmpty().removePrefix("An operation is not implemented: "))
        short.stackTrace = e.stackTrace
        throw short
    }
}
