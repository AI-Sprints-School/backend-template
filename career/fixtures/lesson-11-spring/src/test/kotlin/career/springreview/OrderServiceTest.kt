package career.springreview

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Способ проверки self-invocation тестом (эталон для урока 11): вызываем
 * applyDiscount() напрямую — через прокси — и убеждаемся, что транзакция
 * активна; вызываем placeOrder(), который дёргает applyDiscount() изнутри
 * того же объекта, — и убеждаемся, что внутри self-invocation транзакции
 * нет, хотя метод помечен @Transactional.
 */
@SpringBootTest(classes = [Application::class])
class OrderServiceTest {

    @Autowired
    lateinit var orderService: OrderService

    @Test
    fun `applyDiscount через прокси открывает транзакцию`() {
        val active = orderService.applyDiscount("order-1", 100)
        assertTrue(active, "прямой вызов через бин-прокси должен идти в транзакции")
    }

    @Test
    fun `applyDiscount при self-invocation из placeOrder транзакции не получает`() {
        val result = orderService.placeOrder("order-2", 100)
        assertFalse(
            result.transactionActiveInsideApplyDiscount,
            "self-invocation обходит прокси Spring — @Transactional на applyDiscount() не срабатывает",
        )
    }
}
