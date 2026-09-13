package loader.core

// Имитация внешнего мира загрузчика. Готовая часть шаблона — студент не правит.

import java.io.IOException

/** Файл во внешнем источнике. */
data class RemoteFile(val name: String, val sourceId: String)

/** Загруженные данные файла. */
data class Payload(val file: RemoteFile, val sizeBytes: Int, val bytesSeed: Long)

/** Сбой источника: выбрасывается после задержки источника. */
class SourceUnavailableException(sourceId: String, attempt: Int) :
    IOException("источник $sourceId недоступен (попытка $attempt)")

/** Отказ во входе у callback-клиента (урок 3). */
class AuthException(login: String) : IOException("вход запрещён: $login")

/** Имя потока без суффикса режима отладки: `main @coroutine#3` → `main`. */
fun plainThreadName(name: String = Thread.currentThread().name): String = name.substringBefore(" @")
