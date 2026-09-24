package algorithms.l15

import kotlin.test.Test
import kotlin.test.assertEquals

class IntervalsAndSearchTest {

    @Test
    fun `compressToRanges сжимает подряд идущие числа`() {
        assertEquals("0-5,8-9,11", compressToRanges(listOf(1, 4, 5, 2, 3, 9, 8, 11, 0)))
        assertEquals("1", compressToRanges(listOf(1)))
        assertEquals("", compressToRanges(emptyList()))
    }

    @Test
    fun `mergeIntervals объединяет пересекающиеся и смежные интервалы`() {
        assertEquals(
            listOf(1 to 6, 8 to 10, 15 to 18),
            mergeIntervals(listOf(1 to 3, 2 to 6, 8 to 10, 15 to 18)),
        )
        assertEquals(listOf(1 to 5), mergeIntervals(listOf(1 to 4, 4 to 5)))
        assertEquals(emptyList(), mergeIntervals(emptyList()))
    }

    @Test
    fun `lowerBound находит первый индекс не меньше target`() {
        val values = listOf(1, 3, 3, 5, 7)
        assertEquals(3, lowerBound(values, 4))
        assertEquals(1, lowerBound(values, 3))
        assertEquals(0, lowerBound(values, 0))
        assertEquals(5, lowerBound(values, 100))
    }
}
