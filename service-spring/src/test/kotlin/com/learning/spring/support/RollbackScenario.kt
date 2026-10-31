package com.learning.spring.support

import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * Сценарий отката для урока 23: отметка урока `lessonId` проходит весь код сервиса, но
 * транзакция падает на коммите. Механизм — отложенный триггер на `user_progress`
 * (`DEFERRABLE INITIALLY DEFERRED`): он срабатывает в момент COMMIT, после всего, что сервис
 * успел сделать внутри `@Transactional`, — в том числе после отправки события в брокер.
 *
 * Только для тестов: триггер ставится в базу Testcontainers и снимается методом [clear].
 * Готово в шаблоне.
 */
class RollbackScenario(private val jdbc: JdbcTemplate) {

    fun failCommitFor(lessonId: UUID) {
        jdbc.execute(
            """
            CREATE OR REPLACE FUNCTION test_fail_commit() RETURNS trigger AS ${'$'}${'$'}
            BEGIN
              RAISE EXCEPTION 'Сценарий отката урока 23: коммит отметки урока % отменён', NEW.lesson_id;
            END
            ${'$'}${'$'} LANGUAGE plpgsql
            """.trimIndent(),
        )
        jdbc.execute("DROP TRIGGER IF EXISTS test_fail_commit ON user_progress")
        jdbc.execute(
            """
            CREATE CONSTRAINT TRIGGER test_fail_commit AFTER INSERT ON user_progress
            DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
            WHEN (NEW.lesson_id = '$lessonId'::uuid) EXECUTE FUNCTION test_fail_commit()
            """.trimIndent(),
        )
    }

    fun clear() {
        jdbc.execute("DROP TRIGGER IF EXISTS test_fail_commit ON user_progress")
    }
}
