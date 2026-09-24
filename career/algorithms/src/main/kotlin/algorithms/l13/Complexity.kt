package algorithms.l13

/**
 * Урок 13. Оценка сложности вслух и структуры данных JVM.
 *
 * Задача 1 — над каждой функцией этого файла допишите вторую строку KDoc
 * вида «Сложность: время O(?), память O(?)» с вашей оценкой; сам код этих
 * функций менять не нужно.
 *
 * Задача 2 — функция [hasDuplicatesSlow] ищет повтор в списке за O(n²):
 * `contains()` внутри цикла — самый частый скрытый антипаттерн джуна.
 * Перепишите её тело в [hasDuplicates] так, чтобы алгоритм работал за O(n).
 */

/**
 * Считает количество различных чисел в списке.
 */
fun countDistinct(values: List<Int>): Int = values.toSet().size

/**
 * Возвращает число, которое встречается в списке чаще всего.
 * При равенстве частот — любое из них; на пустом списке — null.
 */
fun mostFrequent(values: List<Int>): Int? {
    if (values.isEmpty()) return null
    val counts = mutableMapOf<Int, Int>()
    for (v in values) counts[v] = (counts[v] ?: 0) + 1
    return counts.maxByOrNull { it.value }?.key
}

/**
 * Скрытый O(n²): на каждой итерации `seen.contains(v)` перебирает список
 * заново. На маленьком входе тесты пройдут — разница видна на большом.
 */
fun hasDuplicatesSlow(values: List<Int>): Boolean {
    val seen = mutableListOf<Int>()
    for (v in values) {
        if (seen.contains(v)) return true
        seen.add(v)
    }
    return false
}

/**
 * То же самое, что [hasDuplicatesSlow], но должно работать за O(n): замените
 * список для «уже виденных» на структуру данных с проверкой присутствия
 * за O(1) в среднем случае.
 */
fun hasDuplicates(values: List<Int>): Boolean {
    TODO("Урок 13: перепишите без contains() в цикле — понадобится другая структура данных")
}
