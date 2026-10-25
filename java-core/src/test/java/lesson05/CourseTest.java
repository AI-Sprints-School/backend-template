package lesson05;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CourseTest {

    @Test
    void constructorStoresIdAndTitleRatingStartsAtZero() {
        Course course = new Course(7L, "Java Core");
        assertEquals(7L, course.getId());
        assertEquals("Java Core", course.getTitle());
        assertEquals(0, course.getRating());
    }

    @Test
    void idIsFinalAndHasNoSetter() throws Exception {
        new Course(1L, "Java Core");
        Field id = Course.class.getDeclaredField("id");
        assertTrue(Modifier.isFinal(id.getModifiers()), "поле id должно быть final");
        boolean hasSetter = Arrays.stream(Course.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("setId"));
        assertFalse(hasSetter, "у id не должно быть сеттера");
    }

    @Test
    void allFieldsArePrivate() {
        new Course(1L, "Java Core");
        for (Field field : Course.class.getDeclaredFields()) {
            if (field.isSynthetic()) continue;
            if (Modifier.isStatic(field.getModifiers())) continue;
            assertTrue(Modifier.isPrivate(field.getModifiers()), "поле " + field.getName() + " должно быть private");
        }
        assertTrue(Course.class.getDeclaredFields().length >= 3, "ожидаются поля id, title, rating");
    }

    @Test
    void nullOrBlankTitleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Course(1L, null));
        assertThrows(IllegalArgumentException.class, () -> new Course(1L, ""));
        assertThrows(IllegalArgumentException.class, () -> new Course(1L, "   "));
    }

    @Test
    void ratingAcceptsZeroToFive() {
        Course course = new Course(1L, "Java Core");
        for (int value = 0; value <= 5; value++) {
            course.setRating(value);
            assertEquals(value, course.getRating());
        }
    }

    @Test
    void ratingOutOfRangeIsRejectedAndKeepsOldValue() {
        Course course = new Course(1L, "Java Core");
        course.setRating(3);
        assertThrows(IllegalArgumentException.class, () -> course.setRating(-1));
        assertThrows(IllegalArgumentException.class, () -> course.setRating(6));
        assertEquals(3, course.getRating());
    }
}
