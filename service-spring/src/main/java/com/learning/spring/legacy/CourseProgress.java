package com.learning.spring.legacy;

/** Строка отчёта: доля пройденных отметок по курсу, в процентах. */
public record CourseProgress(String courseId, String courseTitle, int total, int completed, int percent) {
}
