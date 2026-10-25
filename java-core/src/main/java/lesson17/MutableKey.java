package lesson17;

import java.util.Objects;

/**
 * Урок 17. Ключ для HashMap с изменяемым полем. equals и hashCode написаны
 * верно, дефект — в изменяемости. Покажите его тестом (MutableKeyLookupTest),
 * потом исправьте.
 */
public final class MutableKey {

    private String code;

    public MutableKey(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MutableKey other)) return false;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
