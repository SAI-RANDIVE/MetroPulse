package com.metropulse.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertyResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start when APP_ENV=production but passwords are still placeholders.
 * The error names the variables, never their values.
 */
public class ProductionSafetyCheck implements EnvironmentPostProcessor, Ordered {

    static final int MIN_PRODUCTION_PASSWORD_LENGTH = 12;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> problems = findProblems(environment);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", problems));
        }
    }

    static List<String> findProblems(PropertyResolver env) {
        List<String> problems = new ArrayList<>();
        if (!"production".equalsIgnoreCase(env.getProperty("metropulse.environment", "development"))) {
            return problems;
        }
        checkSecret(env.getProperty("spring.datasource.password", ""), "DB_PASSWORD", problems);
        String adminEmail = env.getProperty("metropulse.security.admin-email", "");
        if (!adminEmail.isBlank()) {
            checkSecret(env.getProperty("metropulse.security.admin-password", ""), "ADMIN_PASSWORD", problems);
        }
        return problems;
    }

    private static void checkSecret(String value, String name, List<String> problems) {
        if (isPlaceholder(value)) {
            problems.add(name + " is empty or still a CHANGE_ME placeholder");
        } else if (value.length() < MIN_PRODUCTION_PASSWORD_LENGTH) {
            problems.add(name + " must be at least " + MIN_PRODUCTION_PASSWORD_LENGTH + " characters in production");
        }
    }

    public static boolean isPlaceholder(String value) {
        return value == null || value.isBlank() || value.toUpperCase().startsWith("CHANGE_ME");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // after .env and application.yml are both available
    }
}
