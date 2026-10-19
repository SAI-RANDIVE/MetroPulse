package com.metropulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MetroPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetroPulseApplication.class, args);
    }
}
