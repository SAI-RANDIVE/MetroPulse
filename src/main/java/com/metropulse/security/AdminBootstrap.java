package com.metropulse.security;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.entity.AppUser;
import com.metropulse.enums.AuditAction;
import com.metropulse.enums.Role;
import com.metropulse.service.AuditService;
import com.metropulse.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator from ADMIN_EMAIL / ADMIN_PASSWORD (read from the environment or .env).
 * The password is BCrypt-hashed by {@link UserService}. An existing account is never overwritten,
 * so changing ADMIN_PASSWORD later does not reset a live password. Nothing secret is logged.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final MetroPulseProperties properties;
    private final UserService users;
    private final AuditService audit;

    public AdminBootstrap(MetroPulseProperties properties, UserService users, AuditService audit) {
        this.properties = properties;
        this.users = users;
        this.audit = audit;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = properties.security().adminEmail();
        String password = properties.security().adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_EMAIL / ADMIN_PASSWORD not set - no administrator account was bootstrapped");
            return;
        }
        if (users.exists(email)) {
            log.info("Administrator bootstrap skipped: the configured account already exists");
            return;
        }
        AppUser admin = users.createUser(properties.security().adminDisplayName(), email, password, Role.ADMIN);
        audit.record(null, AuditAction.ADMIN_BOOTSTRAPPED, "AppUser", admin.getId(), "Administrator account created from configuration");
        log.info("Administrator account created from configuration (id {})", admin.getId());
    }
}
