package com.metropulse.support;

import com.metropulse.entity.AppUser;
import com.metropulse.enums.Role;
import com.metropulse.repository.AppUserRepository;
import com.metropulse.repository.LineStationRepository;
import com.metropulse.repository.MetroLineRepository;
import com.metropulse.repository.StationRepository;
import com.metropulse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

/**
 * Base class for integration tests: full Spring context, MockMvc and a real PostgreSQL database.
 * The .env file is ignored so tests do not depend on a developer's local settings, and demo data is off
 * so every test controls its own data. Tests that write data are @Transactional and roll back.
 */
@SpringBootTest(properties = {
        "metropulse.dotenv.enabled=false",
        "metropulse.environment=test",
        "metropulse.demo.enabled=false",
        "metropulse.security.admin-email=" + IntegrationTest.ADMIN_EMAIL,
        "metropulse.security.admin-password=" + IntegrationTest.ADMIN_PASSWORD,
        "spring.thymeleaf.cache=true"
})
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    public static final String ADMIN_EMAIL = "admin@test.local";
    public static final String ADMIN_PASSWORD = "test-admin-password";
    protected static final String COMMUTER_PASSWORD = "commuter-pass-1";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected UserService userService;
    @Autowired
    protected AppUserRepository users;
    @Autowired
    protected StationRepository stationRepository;
    @Autowired
    protected MetroLineRepository lineRepository;
    @Autowired
    protected LineStationRepository lineStationRepository;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", EmbeddedPostgresSupport::jdbcUrl);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    protected AppUser commuter(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseGet(() -> userService.createUser("Test commuter", email, COMMUTER_PASSWORD, Role.COMMUTER));
    }

    protected AppUser admin() {
        return users.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow();
    }

    protected static RequestPostProcessor asAdmin() {
        return httpBasic(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected static RequestPostProcessor asCommuter(String email) {
        return httpBasic(email, COMMUTER_PASSWORD);
    }

    protected long stationId(String code) {
        return stationRepository.findByCode(code).orElseThrow().getId();
    }

    /** id of the line_station node for a line code and station code. */
    protected long nodeId(String lineCode, String stationCode) {
        long lineId = lineRepository.findByLineCodeIgnoreCase(lineCode).orElseThrow().getId();
        return lineStationRepository.findByLineIdAndStationId(lineId, stationId(stationCode)).orElseThrow().getId();
    }
}
