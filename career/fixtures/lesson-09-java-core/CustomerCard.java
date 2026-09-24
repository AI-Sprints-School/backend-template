package career.javacore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Урок 9 карьерного курса. Ревью Java-класса: equals/hashCode, HashMap,
 * проглоченное исключение. Класс — часть учебной карточки лояльности,
 * которая хранит заказы клиента в HashMap; вложенный OrderTag — метка
 * заказа, живущая в HashSet.
 */
public class CustomerCard {

    private String customerId;
    private int loyaltyPoints;

    public CustomerCard(String customerId, int loyaltyPoints) {
        this.customerId = customerId;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void addPoints(int points) {
        this.loyaltyPoints += points;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CustomerCard)) {
            return false;
        }
        CustomerCard other = (CustomerCard) o;
        return customerId.equals(other.customerId);
    }

    // hashCode() намеренно не переопределён.

    public static final Map<CustomerCard, Integer> ORDERS_BY_CARD = new HashMap<>();

    public static void registerOrder(CustomerCard card, int orderId) {
        try {
            card.addPoints(1); // начисляем бонус за заказ
            ORDERS_BY_CARD.put(card, orderId);
        } catch (Exception e) {
            // молча проглочено
        }
    }

    public static Integer findOrder(CustomerCard card) {
        return ORDERS_BY_CARD.get(card);
    }

    /**
     * Метка заказа. equals()/hashCode() корректно согласованы по label — но
     * label изменяемо, и объект уже может лежать в HashSet как ключ.
     */
    public static final class OrderTag {
        private String label;

        public OrderTag(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void relabel(String label) {
            this.label = label;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OrderTag)) {
                return false;
            }
            return label.equals(((OrderTag) o).label);
        }

        @Override
        public int hashCode() {
            return label.hashCode();
        }
    }

    // Индекс активных меток заказов смены. OrderTag корректно переопределяет
    // equals()/hashCode() по label, но если label меняется уже после
    // вставки объекта в HashSet как ключ, запись не переезжает в новый
    // бакет сама: findById() с тем же объектом её больше не находит.
    public static final Set<OrderTag> ACTIVE_TAGS = new HashSet<>();

    public static void index(OrderTag tag) {
        ACTIVE_TAGS.add(tag);
    }

    public static boolean findById(OrderTag tag) {
        return ACTIVE_TAGS.contains(tag);
    }
}
