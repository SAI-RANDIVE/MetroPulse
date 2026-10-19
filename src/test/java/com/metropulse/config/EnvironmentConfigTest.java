package com.metropulse.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The root .env loading and the production safety guard. */
class EnvironmentConfigTest {

    @Test
    void parsesKeyValueLinesCommentsAndQuotes() {
        Map<String, Object> values = DotenvFile.parse(List.of(
                "# comment", "", "APP_PORT=9090", "export DB_NAME=metro", "DB_PASSWORD=\"with spaces # not a comment\"",
                "EMPTY=", "TRAILING=value # comment", "not a pair"));
        assertThat(values).containsEntry("APP_PORT", "9090").containsEntry("DB_NAME", "metro")
                .containsEntry("DB_PASSWORD", "with spaces # not a comment")
                .containsEntry("EMPTY", "").containsEntry("TRAILING", "value")
                .doesNotContainKey("not a pair");
    }

    @Test
    void dotenvOverridesDefaultsButNotRealEnvironmentVariables(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(".env");
        Files.writeString(file, "DB_NAME=from_dotenv\nAPP_PORT=7000\n");

        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        Map.of("APP_PORT", "8000")));
        env.getPropertySources().addLast(new MapPropertySource("defaults", Map.of("DB_NAME", "default")));
        env.getPropertySources().addFirst(new MapPropertySource("cli",
                Map.of(DotenvEnvironmentPostProcessor.PATH_PROPERTY, file.toString())));

        new DotenvEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("DB_NAME")).isEqualTo("from_dotenv"); // beats the default
        assertThat(env.getProperty("APP_PORT")).isEqualTo("8000");       // the OS variable wins
        assertThat(env.getPropertySources().contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isTrue();
    }

    @Test
    void missingDotenvIsNotAnError(@TempDir Path dir) {
        MockEnvironment env = new MockEnvironment().withProperty(DotenvEnvironmentPostProcessor.PATH_PROPERTY, dir.resolve("none").toString());
        new DotenvEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(env, new SpringApplication());
        assertThat(env.getPropertySources().contains(DotenvEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
    }

    @Test
    void productionRefusesPlaceholderSecretsWithoutPrintingThem() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("metropulse.environment", "production")
                .withProperty("spring.datasource.password", "CHANGE_ME_LOCAL")
                .withProperty("metropulse.security.admin-email", "admin@x.org")
                .withProperty("metropulse.security.admin-password", "short");
        List<String> problems = ProductionSafetyCheck.findProblems(env);
        assertThat(problems).hasSize(2).anyMatch(p -> p.startsWith("DB_PASSWORD")).anyMatch(p -> p.startsWith("ADMIN_PASSWORD"));
        assertThat(String.join(" ", problems)).doesNotContain("CHANGE_ME_LOCAL").doesNotContain("short ");
        assertThatThrownBy(() -> new ProductionSafetyCheck().postProcessEnvironment(env, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void developmentAllowsLocalPlaceholders() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("metropulse.environment", "development")
                .withProperty("spring.datasource.password", "CHANGE_ME_LOCAL");
        assertThat(ProductionSafetyCheck.findProblems(env)).isEmpty();
        MockEnvironment strong = new MockEnvironment()
                .withProperty("metropulse.environment", "production")
                .withProperty("spring.datasource.password", "a-long-real-password");
        assertThat(ProductionSafetyCheck.findProblems(strong)).isEmpty();
    }
}
