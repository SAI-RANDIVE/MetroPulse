package com.metropulse.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Spring Boot does not read ".env" files by itself. This post-processor loads the root ".env"
 * (the folder the application is started from, i.e. next to pom.xml) into the Spring Environment.
 * <p>
 * Priority: OS environment variables and -D properties win over .env, and .env wins over the
 * defaults in application.yml. Registered in META-INF/spring.factories.
 * Values are never logged - only the number of keys.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "dotenv";
    /** Optional override of the file location, e.g. -Dmetropulse.dotenv.path=C:/config/.env */
    public static final String PATH_PROPERTY = "metropulse.dotenv.path";
    /** Set to false (e.g. in tests) to skip loading the file. */
    public static final String ENABLED_PROPERTY = "metropulse.dotenv.enabled";

    private final Log log;

    public DotenvEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(DotenvEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty(ENABLED_PROPERTY, Boolean.class, true)) {
            return;
        }
        Path file = Path.of(environment.getProperty(PATH_PROPERTY, ".env")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            log.info("No .env file found at " + file + " - using environment variables and defaults");
            return;
        }
        Map<String, Object> values;
        try {
            values = DotenvFile.read(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
        addAfterSystemEnvironment(environment.getPropertySources(), new MapPropertySource(PROPERTY_SOURCE_NAME, values));
        log.info("Loaded " + values.size() + " settings from " + file);
    }

    static void addAfterSystemEnvironment(MutablePropertySources sources, MapPropertySource dotenv) {
        String systemEnv = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        if (sources.contains(systemEnv)) {
            sources.addAfter(systemEnv, dotenv);
        } else {
            sources.addLast(dotenv);
        }
    }

    @Override
    public int getOrder() {
        // Before ConfigDataEnvironmentPostProcessor so application.yml placeholders can use .env values
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
