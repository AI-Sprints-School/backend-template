package loader.lesson07

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import loader.core.Payload
import loader.core.RemoteFile
import loader.core.SourceCatalog
import java.util.concurrent.atomic.AtomicInteger

// Урок 7. Пакет загрузок в структурированной конкурентности.

/** coroutineScope + async на файл + awaitAll; после каждой загрузки — completed.incrementAndGet(). */
suspend fun downloadBatch(files: List<RemoteFile>, catalog: SourceCatalog, completed: AtomicInteger): List<Payload> {
    TODO("Урок 07: загрузить пакет в coroutineScope и считать готовые загрузки")
}
