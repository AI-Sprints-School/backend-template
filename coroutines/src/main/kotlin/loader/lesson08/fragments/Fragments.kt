package loader.lesson08.fragments

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import loader.core.log
import java.io.IOException

// Готово в шаблоне, не правится. Три фрагмента для предсказания: где окажется исключение?
// Каждый получает обработчик корня и пишет в лог, куда дошло исключение.

/** Фрагмент 1: исключение в launch корневого scope с обработчиком. */
suspend fun fragmentLaunch(handler: CoroutineExceptionHandler) {
    val job = CoroutineScope(Job() + handler).launch {
        throw IOException("фрагмент 1: сбой в launch")
    }
    job.join()
    log("фрагмент 1: join вернулся, задача отменена = ${job.isCancelled}")
}

/** Фрагмент 2: исключение в async, await в try/catch внутри coroutineScope. */
suspend fun fragmentAsync(handler: CoroutineExceptionHandler) {
    coroutineScope {
        val deferred = async(handler) { throw IOException("фрагмент 2: сбой в async") }
        try {
            deferred.await()
        } catch (e: IOException) {
            log("фрагмент 2: пойман у await — ${e.message}")
        }
    }
    log("фрагмент 2: coroutineScope вернулся")
}

/** Фрагмент 3: исключение в launch внутри coroutineScope, try/catch вокруг coroutineScope. */
suspend fun fragmentNested(handler: CoroutineExceptionHandler) {
    try {
        coroutineScope {
            launch(handler) { throw IOException("фрагмент 3: сбой во вложенном launch") }
        }
        log("фрагмент 3: coroutineScope вернулся")
    } catch (e: IOException) {
        log("фрагмент 3: пойман вокруг coroutineScope — ${e.message}")
    }
}
