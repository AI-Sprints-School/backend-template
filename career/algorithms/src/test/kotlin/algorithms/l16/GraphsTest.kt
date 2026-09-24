package algorithms.l16

import kotlin.test.Test
import kotlin.test.assertEquals

class GraphsTest {

    // 0 - 1 - 2
    // |       |
    // 3 - - - 4    5 (отдельная компонента)
    private val graph = mapOf(
        0 to listOf(1, 3),
        1 to listOf(0, 2),
        2 to listOf(1, 4),
        3 to listOf(0, 4),
        4 to listOf(2, 3),
        5 to emptyList(),
    )

    @Test
    fun `находит кратчайший путь числом рёбер`() {
        assertEquals(2, shortestPathLength(graph, 0, 2))
        assertEquals(1, shortestPathLength(graph, 0, 3))
        assertEquals(2, shortestPathLength(graph, 0, 4))
    }

    @Test
    fun `старт равен финишу — путь нулевой длины`() {
        assertEquals(0, shortestPathLength(graph, 2, 2))
    }

    @Test
    fun `недостижимая вершина — минус один`() {
        assertEquals(-1, shortestPathLength(graph, 0, 5))
    }
}
