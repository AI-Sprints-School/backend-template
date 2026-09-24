package algorithms.l13

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComplexityTest {

    @Test
    fun `countDistinct считает уникальные числа`() {
        assertEquals(3, countDistinct(listOf(1, 2, 2, 3, 1, 3, 3)))
        assertEquals(0, countDistinct(emptyList()))
    }

    @Test
    fun `mostFrequent находит самое частое число`() {
        assertEquals(2, mostFrequent(listOf(1, 2, 2, 3, 2)))
        assertEquals(null, mostFrequent(emptyList()))
    }

    @Test
    fun `hasDuplicates находит повтор`() {
        assertTrue(hasDuplicates(listOf(1, 2, 3, 2)))
        assertFalse(hasDuplicates(listOf(1, 2, 3)))
        assertFalse(hasDuplicates(emptyList()))
    }

    @Test
    fun `hasDuplicates укладывается в разумное время на большом входе`() {
        // 300 000 уникальных чисел — худший случай для O(n^2): contains()
        // на списке такого размера не укладывается и в разумные секунды,
        // а O(n) на Set проходит на обычной машине заметно меньше секунды.
        val big = (1..300_000).toList()
        val elapsedMs = measureTimeMillis { hasDuplicates(big) }
        assertTrue(
            elapsedMs < 3_000,
            "заняло $elapsedMs мс на 300 000 элементов — похоже на O(n²), а не на O(n)",
        )
    }
}
