package algorithms.l14

import kotlin.test.Test
import kotlin.test.assertEquals

class HashAndWindowTest {

    @Test
    fun `longestSubstringWithoutRepeats считает окно без повторов`() {
        assertEquals(3, longestSubstringWithoutRepeats("abcabcbb"))
        assertEquals(1, longestSubstringWithoutRepeats("bbbbb"))
        assertEquals(3, longestSubstringWithoutRepeats("pwwkew"))
        assertEquals(0, longestSubstringWithoutRepeats(""))
    }

    @Test
    fun `twoSum находит пару с заданной суммой`() {
        assertEquals(0 to 1, twoSum(listOf(2, 7, 11, 15), 9))
        assertEquals(1 to 2, twoSum(listOf(3, 2, 4), 6))
        assertEquals(0 to 1, twoSum(listOf(3, 3), 6))
    }

    @Test
    fun `groupAnagrams группирует слова с одинаковым набором букв`() {
        val result = groupAnagrams(listOf("eat", "tea", "tan", "ate", "nat", "bat"))
            .map { it.toSet() }
            .toSet()
        val expected = setOf(
            setOf("eat", "tea", "ate"),
            setOf("tan", "nat"),
            setOf("bat"),
        )
        assertEquals(expected, result)
    }

    @Test
    fun `groupAnagrams на пустом входе возвращает пустой список`() {
        assertEquals(emptyList(), groupAnagrams(emptyList()))
    }
}
