package com.learning.spring.legacy;

/** Одна отметка прогресса: ученик прошёл (или не прошёл) урок курса. */
public record ProgressEntry(String userId, String courseId, String courseTitle, boolean completed) {
}
