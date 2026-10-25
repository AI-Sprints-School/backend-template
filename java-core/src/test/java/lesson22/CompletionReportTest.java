package lesson22;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CompletionReportTest {

    private static final double EPS = 1e-9;

    @Test
    void shareByCourseGroupsAndDividesSums() {
        Map<String, Double> share = CompletionReport.completionShareByCourse(CatalogFixture.enrollments());
        assertEquals(3, share.size());
        assertEquals(22.0 / 30.0, share.get("java-core"), EPS);
        assertEquals(9.0 / 24.0, share.get("spring-boot"), EPS);
        assertEquals(1.0, share.get("kotlin-coroutines"), EPS);
    }

    @Test
    void shareByCourseOfNothingIsEmpty() {
        assertTrue(CompletionReport.completionShareByCourse(List.of()).isEmpty());
    }

    @Test
    void partitionSplitsByThresholdInclusive() {
        Map<Boolean, Long> parts = CompletionReport.partitionByThreshold(CatalogFixture.enrollments(), 0.5);
        assertEquals(4L, parts.get(true), "s1/java, s3/java, s2/spring (ровно 0.5), s3/coroutines");
        assertEquals(2L, parts.get(false));
    }

    @Test
    void partitionKeepsBothKeysWhenOneSideIsEmpty() {
        Map<Boolean, Long> parts = CompletionReport.partitionByThreshold(CatalogFixture.enrollments(), 1.1);
        assertEquals(0L, parts.get(true));
        assertEquals(6L, parts.get(false));
    }

    @Test
    void titlesBySlugKeepsFirstOnDuplicate() {
        Map<String, String> titles = CompletionReport.titlesBySlug(CatalogFixture.courses());
        assertEquals(3, titles.size());
        assertEquals("Java Core", titles.get("java-core"));
        assertEquals("Spring Boot", titles.get("spring-boot"));
    }
}
