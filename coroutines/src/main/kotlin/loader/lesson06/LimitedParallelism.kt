package loader.lesson06

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import loader.core.RemoteFile
import loader.core.SourceCatalog

// Готово в шаблоне, не правится: вместо Semaphore — Dispatchers.IO.limitedParallelism(parallelism).
// Сравните maxInFlight источников с вашим downloadAndStore.

suspend fun downloadWithLimitedParallelism(files: List<RemoteFile>, catalog: SourceCatalog, parallelism: Int = 3) {
    val limited = Dispatchers.IO.limitedParallelism(parallelism)
    coroutineScope {
        files.map { file ->
            async(CoroutineName("limited-${file.name}")) {
                withContext(limited) { catalog.source(file.sourceId).download(file) }
            }
        }.awaitAll()
    }
}
