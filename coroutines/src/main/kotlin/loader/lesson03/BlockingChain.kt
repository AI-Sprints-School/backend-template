package loader.lesson03

import loader.core.CallbackSourceClient
import loader.core.RemoteFile
import loader.core.RequestKind
import loader.core.log

// Готово в шаблоне, не правится: та же цепочка на блокирующем requestBlocking — режим 2 сценария.

fun fetchFileBlocking(client: CallbackSourceClient, login: String, file: RemoteFile): FetchedFile {
    val token = client.requestBlocking(RequestKind.AUTH, login)
    log("цепочка ${file.name}: токен получен")
    val metadata = client.requestBlocking(RequestKind.METADATA, file.name)
    log("цепочка ${file.name}: метаданные получены")
    val content = client.requestBlocking(RequestKind.FILE, file.name)
    log("цепочка ${file.name}: файл получен")
    return FetchedFile(token, metadata, content)
}
