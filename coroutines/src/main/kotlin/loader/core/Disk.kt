package loader.core

import kotlinx.coroutines.delay
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Записанный на диск файл. */
data class StoredFile(val name: String, val sizeBytes: Int, val checksum: Long)

/** Имитация диска: блокирующие запись и чтение, временные файлы. */
class FakeDisk(val ioDelayMs: Long = 50) {
    private val temps = ConcurrentHashMap.newKeySet<String>()
    private val deleted = Collections.synchronizedList(mutableListOf<String>())
    private val writes = Collections.synchronizedList(mutableListOf<String>())
    private val reads = Collections.synchronizedList(mutableListOf<String>())
    private val files = ConcurrentHashMap<String, List<String>>()
    private val tempNumber = AtomicInteger()

    /** Блокирующая запись: `Thread.sleep(ioDelayMs)`, запоминает поток. Контрольная сумма — 0. */
    fun writeBlocking(name: String, payload: Payload): StoredFile {
        Thread.sleep(ioDelayMs)
        writes += plainThreadName()
        return StoredFile(name, payload.sizeBytes, checksum = 0)
    }

    /** Имя нового временного файла. */
    fun createTemp(name: String): String {
        val temp = "$name.tmp${tempNumber.incrementAndGet()}"
        temps += temp
        return temp
    }

    /** Удаление временного файла с приостановкой: `delay(ioDelayMs)`, затем удаляет. */
    suspend fun deleteTemp(tempName: String) {
        delay(ioDelayMs)
        temps -= tempName
        deleted += tempName
        log("диск: удалён $tempName")
    }

    /** Кладёт строки файла, который потом читает [readLinesBlocking]. */
    fun putLines(name: String, lines: List<String>) {
        files[name] = lines.toList()
    }

    /** Блокирующее чтение: `Thread.sleep(ioDelayMs)`, запоминает поток. */
    fun readLinesBlocking(name: String): List<String> {
        Thread.sleep(ioDelayMs)
        reads += plainThreadName()
        return files[name] ?: error("на диске нет файла $name")
    }

    val tempFiles: Set<String> get() = temps.toSet()
    val deletedTemps: List<String> get() = synchronized(deleted) { deleted.toList() }
    val writeThreads: List<String> get() = synchronized(writes) { writes.toList() }
    val readThreads: List<String> get() = synchronized(reads) { reads.toList() }
}

/** Контрольная сумма — чистый цикл без приостановок. Запоминает потоки вызова. */
object Checksum {
    private val threads = ConcurrentHashMap.newKeySet<String>()

    fun compute(payload: Payload, iterations: Int = 5_000_000): Long {
        threads += plainThreadName()
        var acc = payload.bytesSeed xor payload.sizeBytes.toLong()
        for (i in 0 until iterations) {
            acc = acc * 6364136223846793005L + 1442695040888963407L + i
        }
        return acc
    }

    /** Потоки, на которых вызывался [compute], без суффикса отладки. */
    val callThreads: Set<String> get() = threads.toSet()

    fun reset() = threads.clear()
}

/** Журнал сервиса: запись с приостановкой. */
class FakeJournal(val writeDelayMs: Long = 100) {
    private val lines = Collections.synchronizedList(mutableListOf<String>())

    suspend fun write(line: String) {
        delay(writeDelayMs)
        lines += line
    }

    val records: List<String> get() = synchronized(lines) { lines.toList() }
}
