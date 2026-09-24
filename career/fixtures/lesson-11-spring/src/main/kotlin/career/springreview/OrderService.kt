package career.springreview

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Урок 11 карьерного курса. Фрагмент с self-invocation: placeOrder()
 * вызывает applyDiscount() как обычный метод того же объекта (this.),
 * а не через прокси Spring — аннотация @Transactional на applyDiscount()
 * в этом вызове не сработает.
 */
@Service
class OrderService {

    fun placeOrder(orderId: String, amount: Int): OrderResult {
        val activeBeforeCall = TransactionSynchronizationManager.isActualTransactionActive()
        val activeInsideCall = applyDiscount(orderId, amount)
        return OrderResult(
            orderId = orderId,
            transactionActiveBeforePlaceOrder = activeBeforeCall,
            transactionActiveInsideApplyDiscount = activeInsideCall,
        )
    }

    @Transactional
    fun applyDiscount(orderId: String, amount: Int): Boolean {
        // В реальном коде здесь были бы записи в БД, которым нужна транзакция.
        return TransactionSynchronizationManager.isActualTransactionActive()
    }
}

data class OrderResult(
    val orderId: String,
    val transactionActiveBeforePlaceOrder: Boolean,
    val transactionActiveInsideApplyDiscount: Boolean,
)
