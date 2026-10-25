package lesson25;

/** Студент с баллами. Готовая часть шаблона — править не нужно. */
public final class Student {

    private final String id;
    private int points;

    public Student(String id, int points) {
        this.id = id;
        this.points = points;
    }

    public String getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    void setPoints(int points) {
        this.points = points;
    }
}
