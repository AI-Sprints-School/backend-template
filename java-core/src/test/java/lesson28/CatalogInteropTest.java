package lesson28;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogInteropTest {

    @Test
    void searchWithoutLimitUsesDefaultOfTen() {
        List<String> found = CatalogClient.firstMatches("java");
        assertEquals(10, found.size());
    }

    @Test
    void emptyQueryMessageIsPlainStaticField() throws Exception {
        assertEquals("Запрос не может быть пустым", CatalogClient.emptyQueryMessage());
        Field field = CatalogService.class.getField("EMPTY_QUERY_MESSAGE");
        assertTrue(Modifier.isStatic(field.getModifiers()), "@JvmField на свойстве companion даёт static-поле");
    }

    @Test
    void factoriesAreStaticMethodsOfService() throws Exception {
        Method create = CatalogService.class.getMethod("create", List.class);
        Method demo = CatalogService.class.getMethod("withDemoData");
        assertTrue(Modifier.isStatic(create.getModifiers()), "create: нужен @JvmStatic");
        assertTrue(Modifier.isStatic(demo.getModifiers()), "withDemoData: нужен @JvmStatic");
    }

    @Test
    void searchHasOverloadWithoutLimit() throws Exception {
        assertNotNull(CatalogService.class.getMethod("search", String.class), "search(String): нужен @JvmOverloads");
    }

    @Test
    void titleOrDashHandlesNull() {
        CatalogService service = CatalogService.Companion.withDemoData();
        assertEquals("Java, урок 1", CatalogClient.titleOrDash(service, 0));
        assertEquals("нет", CatalogClient.titleOrDash(service, 99));
    }

    @Test
    void mavenDependencyIsMovedToGradle() throws Exception {
        String pom = Files.readString(Path.of("maven/lesson28/pom.xml"));
        String group = tag(pom, "groupId", "commons-lang3");
        String version = tag(pom, "version", "commons-lang3");
        String gradle = Files.readString(Path.of("build.gradle.kts"));
        String coordinate = group + ":commons-lang3:" + version;
        boolean declared = gradle.lines().anyMatch(l -> l.contains("implementation(\"" + coordinate + "\")"));
        assertTrue(declared, "в build.gradle.kts нет implementation(\"" + coordinate + "\")");
    }

    /** Значение тега из блока dependency, в котором встречается artifactId. */
    private static String tag(String pom, String name, String artifactId) {
        int at = pom.indexOf("<artifactId>" + artifactId + "</artifactId>");
        int start = pom.lastIndexOf("<dependency>", at);
        int end = pom.indexOf("</dependency>", at);
        String block = pom.substring(start, end);
        int from = block.indexOf("<" + name + ">") + name.length() + 2;
        return block.substring(from, block.indexOf("</" + name + ">"));
    }
}
