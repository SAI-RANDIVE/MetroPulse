package com.metropulse.config;

import com.metropulse.service.DemoDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Seeds demonstration data at startup when DEMO_DATA_ENABLED=true (runs after the admin bootstrap). */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private final DemoDataService demoData;
    private final MetroPulseProperties properties;

    public DemoDataSeeder(DemoDataService demoData, MetroPulseProperties properties) {
        this.demoData = demoData;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.demo().enabled()) {
            demoData.seedIfEmpty();
        }
    }
}
