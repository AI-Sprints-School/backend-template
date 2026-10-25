package lesson28

/**
 * Урок 28. Kotlin-сервис каталога, выдан как есть. Из Java им неудобно
 * пользоваться: нужен `.Companion`, нет перегрузок, константа за геттером.
 * Расставьте аннотации (@JvmStatic, @JvmOverloads, @JvmField) так, чтобы
 * Java-код в CatalogClient.java обходился без `Companion`.
 */
class CatalogService private constructor(private val titles: List<String>) {

    fun search(query: String, limit: Int = DEFAULT_LIMIT): List<String> {
        require(query.isNotBlank()) { EMPTY_QUERY_MESSAGE }
        return titles.filter { it.contains(query, ignoreCase = true) }.take(limit)
    }

    fun titleOrNull(index: Int): String? = titles.getOrNull(index)

    companion object {
        const val DEFAULT_LIMIT = 10

        val EMPTY_QUERY_MESSAGE = "Запрос не может быть пустым"

        fun create(titles: List<String>): CatalogService = CatalogService(titles)

        fun withDemoData(): CatalogService =
            create((1..12).map { "Java, урок $it" } + listOf("Kotlin, урок 1"))
    }
}
