package com.metropulse.web;

import com.metropulse.enums.Role;
import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class SecurityIntegrationTest extends IntegrationTest {

    @Test
    void publicPagesAndApiAreOpen() throws Exception {
        for (String path : new String[]{"/", "/plan", "/stations", "/lines/2A", "/alerts", "/login", "/register"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/lines")).andExpect(status().isOk());
        mvc.perform(get("/api/alerts")).andExpect(status().isOk());
    }

    @Test
    void adminApiNeedsAuthentication() throws Exception {
        mvc.perform(get("/api/admin/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void commuterCannotUseAdminApi() throws Exception {
        commuter("plain@test.local");
        mvc.perform(get("/api/admin/reports").with(asCommuter("plain@test.local")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(patch("/api/admin/reports/1/verification").with(asCommuter("plain@test.local"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminApi() throws Exception {
        mvc.perform(get("/api/admin/dashboard/stats").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCount").value(3))
                .andExpect(jsonPath("$.stationCount").value(42))
                .andExpect(jsonPath("$.networkIssues").isEmpty());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/admin/reports").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic(ADMIN_EMAIL, "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPagesRedirectAnonymousAndForbidCommuters() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin").with(user("c@test.local").roles("COMMUTER"))).andExpect(status().isForbidden());
        mvc.perform(get("/reports/new")).andExpect(status().is3xxRedirection());
    }

    @Test
    void formsRequireCsrfToken() throws Exception {
        mvc.perform(post("/reports").with(user("c@test.local").roles("COMMUTER"))
                        .param("lineStationId", "1").param("crowdLevel", "LOW"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationAlwaysCreatesCommuterEvenIfRoleIsPosted() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "Sneaky").param("email", "sneaky@test.local")
                        .param("password", "password123").param("confirmPassword", "password123")
                        .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        var created = users.findByEmailIgnoreCase("sneaky@test.local").orElseThrow();
        assertThat(created.getRole()).isEqualTo(Role.COMMUTER);
        assertThat(created.getPasswordHash()).startsWith("$2").doesNotContain("password123");
    }

    @Test
    void duplicateAccountAndMismatchedPasswordsAreShown() throws Exception {
        commuter("taken@test.local");
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "Again").param("email", "TAKEN@test.local")
                        .param("password", "password123").param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
        mvc.perform(post("/register").with(csrf())
                        .param("displayName", "X").param("email", "new@test.local")
                        .param("password", "password123").param("confirmPassword", "different1"))
                .andExpect(content().string(containsString("Passwords do not match")));
    }

    @Test
    void adminIsBootstrappedWithHashedPassword() {
        var admin = admin();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getPasswordHash()).startsWith("$2").isNotEqualTo(ADMIN_PASSWORD);
    }

    @Test
    void unknownPagesGive404Page() throws Exception {
        mvc.perform(get("/stations/987654")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("isn't on our map")));
    }
}
