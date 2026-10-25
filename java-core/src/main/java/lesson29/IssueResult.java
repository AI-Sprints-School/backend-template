package lesson29;

/** Результат выдачи — аналог sealed class из Kotlin-оригинала. Часть выданной формы. */
public sealed interface IssueResult {

    record Issued(Certificate certificate) implements IssueResult {
    }

    record Rejected(String reason) implements IssueResult {
    }
}
