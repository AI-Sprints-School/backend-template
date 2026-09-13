package loader.lesson03

import kotlinx.coroutines.suspendCancellableCoroutine
import loader.core.CallbackSourceClient
import loader.core.RemoteFile
import loader.core.RequestKind
import loader.core.log
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Урок 3. Callback-клиент источника как suspend-функция.

/** Результат цепочки запросов. */
data class FetchedFile(val token: String, val metadata: String, val content: String)

/**
 * suspendCancellableCoroutine: client.request; onResult → resume, onError → resumeWithException;
 * invokeOnCancellation → cancel() у RequestHandle.
 */
suspend fun call(client: CallbackSourceClient, kind: RequestKind, arg: String): String {
    TODO("Урок 03: обернуть client.request в suspendCancellableCoroutine")
}

/**
 * AUTH(login) → METADATA(file.name) → FILE(file.name) через call. После шагов — log:
 * «цепочка <файл>: токен получен», «цепочка <файл>: метаданные получены», «цепочка <файл>: файл получен».
 */
suspend fun fetchFile(client: CallbackSourceClient, login: String, file: RemoteFile): FetchedFile {
    TODO("Урок 03: получить файл цепочкой AUTH → METADATA → FILE через call")
}
