package com.metropulse;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs MetroPulse with a bundled PostgreSQL server - handy when PostgreSQL is not installed.
 * <pre>
 *   mvn spring-boot:test-run
 * </pre>
 * Data is kept in the ".localdb" folder (git-ignored), so it survives restarts. Delete that folder for a fresh database.
 * The rest of the configuration (admin account, demo data, port) still comes from .env.
 * This class lives in src/test so the embedded database never ships in the real application.
 */
public final class LocalDevApplication {

    private static final int PORT = 54329;

    private LocalDevApplication() {
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Path.of(".localdb").toAbsolutePath();
        Files.createDirectories(dataDir);
        EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                .setDataDirectory(dataDir)
                .setCleanDataDirectory(false)
                .setPort(PORT)
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (IOException ignored) {
                // shutting down anyway
            }
        }));
        // JVM properties take priority over .env, so only the database connection is redirected
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        MetroPulseApplication.main(args);
    }
}
