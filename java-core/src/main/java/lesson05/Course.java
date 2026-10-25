package lesson05;

/**
 * Урок 5. Перепишите Kotlin-класс из samples/lesson05/Course.kt на Java.
 * Требования — в задании урока; тесты — src/test/java/lesson05/CourseTest.java.
 */
public class Course {

    // TODO урок 5: private-поля (id — final, сеттера у id нет), конструктор
    // (id, title) с проверкой title (null или пустой — IllegalArgumentException),
    // геттеры getId, getTitle, getRating, сеттер setRating (0..5, иначе
    // IllegalArgumentException). Сеттер setTitle по желанию.

    public Course(long id, String title) {
        throw new UnsupportedOperationException("Урок 5: конструктор Course не написан");
    }

    public long getId() {
        throw new UnsupportedOperationException("Урок 5: getId не написан");
    }

    public String getTitle() {
        throw new UnsupportedOperationException("Урок 5: getTitle не написан");
    }

    public int getRating() {
        throw new UnsupportedOperationException("Урок 5: getRating не написан");
    }

    public void setRating(int rating) {
        throw new UnsupportedOperationException("Урок 5: setRating не написан");
    }
}
