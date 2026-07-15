package com.pronurse.db;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FlywayMigrationTest {

    @Test
    void testFlywayMigrationsFromEmptyDatabase() {
        assertDoesNotThrow(() -> {
            Flyway flyway = Flyway.configure()
                    .dataSource("jdbc:h2:mem:flywaytest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")
                    .locations("classpath:db/migration")
                    .load();
            flyway.migrate();
        });
    }
}
