package lesson10;

/**
 * Урок 10. Ключ кеша «пользователь + курс». Класс выдан с дефектом —
 * найдите его своим тестом (CacheKeyContractTest), потом исправьте.
 */
public final class CacheKey {

    private final String userId;
    private final String courseId;

    public CacheKey(String userId, String courseId) {
        this.userId = userId;
        this.courseId = courseId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCourseId() {
        return courseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheKey other)) return false;
        return userId.equals(other.userId) && courseId.equals(other.courseId);
    }
}
