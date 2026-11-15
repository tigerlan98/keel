package io.github.keel;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.*;
import java.sql.*;
import java.util.List;

/** Verify the actual V9004 upgrade path, not only a fresh database. */
class DepartmentMigrationTest {
    @Test
    void existingDatabaseCanApplyLowerVersionAdditionsWithoutReassigningUsers() throws Exception {
        Path legacy = Files.createTempDirectory("keel-v9004-");
        try (var mysql =
                new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                        .withDatabaseName("upgrade")
                        .withUsername("keel")
                        .withPassword("keel")) {
            for (String module : List.of("iam", "system", "audit", "seed")) {
                String project = module.equals("seed") ? "keel-bootstrap" : "keel-" + module;
                Path directory = Path.of("..", project, "src/main/resources/db/migration", module);
                try (var files = Files.list(directory)) {
                    for (Path file :
                            files.filter(path -> path.toString().endsWith(".sql")).toList()) {
                        if (!file.getFileName().toString().matches("V(1006|1007|9005)__.*"))
                            Files.copy(file, legacy.resolve(file.getFileName()));
                    }
                }
            }
            mysql.start();
            Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("filesystem:" + legacy)
                    .load()
                    .migrate();
            String[] locations = {
                "classpath:db/migration/iam",
                "classpath:db/migration/system",
                "classpath:db/migration/audit",
                "classpath:db/migration/seed"
            };
            var upgrade =
                    Flyway.configure()
                            .dataSource(
                                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                            .locations(locations)
                            .outOfOrder(true)
                            .load();
            assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(3);
            assertThat(upgrade.migrate().migrationsExecuted).isZero();
            Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations(locations)
                    .outOfOrder(false)
                    .load()
                    .validate();
            try (var connection =
                            DriverManager.getConnection(
                                    mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
                    var stmt = connection.createStatement()) {
                try (var rows =
                        stmt.executeQuery(
                                "SELECT COUNT(*) FROM iam_department WHERE code='ROOT' AND"
                                    + " status='ENABLED'")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(1);
                }
                try (var rows =
                        stmt.executeQuery(
                                "SELECT department_id FROM iam_user WHERE username='admin'")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getObject(1)).isNull();
                }
                try (var rows =
                        stmt.executeQuery(
                                "SELECT COUNT(*) FROM iam_resource WHERE code LIKE"
                                    + " 'iam:department:%'")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(5);
                }
            }
        } finally {
            try (var files = Files.list(legacy)) {
                for (var file : files.toList()) Files.delete(file);
            }
            Files.delete(legacy);
        }
    }
}
