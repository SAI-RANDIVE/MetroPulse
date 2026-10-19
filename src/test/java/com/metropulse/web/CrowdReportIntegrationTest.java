package com.metropulse.web;

import com.metropulse.entity.CrowdReport;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.repository.CrowdReportRepository;
import com.metropulse.service.CrowdStatusService;
import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
class CrowdReportIntegrationTest extends IntegrationTest {

    private static final String EMAIL = "reporter@test.local";

    @Autowired
    CrowdReportRepository reports;
    @Autowired
    CrowdStatusService crowdStatus;

    private String json(long node, String level) {
        return "{\"lineStationId\":" + node + ",\"crowdLevel\":\"" + level + "\",\"comment\":\"  busy  \"}";
    }

    @Test
    void commuterReportIsStoredAsPendingWithExpiry() throws Exception {
        var user = commuter(EMAIL);
        long node = nodeId("1", "ANDHERI");
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(json(node, "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.level").value("HIGH"))
                .andExpect(jsonPath("$.comment").value("busy"))
                .andExpect(jsonPath("$.stationName").value("Andheri"));

        CrowdReport saved = reports.findAll().stream().filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        assertThat(Duration.between(saved.getCreatedAt(), saved.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(saved.getVerifiedBy()).isNull();
        assertThat(crowdStatus.currentLevels()).containsEntry(node, CrowdLevel.HIGH);

        mvc.perform(get("/api/crowd-reports/mine").with(asCommuter(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void clientCannotOverPostStatusOrUser() throws Exception {
        commuter(EMAIL);
        String body = "{\"lineStationId\":" + nodeId("1", "CHAKALA")
                + ",\"crowdLevel\":\"LOW\",\"verificationStatus\":\"APPROVED\",\"userId\":1,\"verifiedBy\":1}";
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.verifiedByName").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void invalidReportGivesFieldErrors() throws Exception {
        commuter(EMAIL);
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"" + "x".repeat(300) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("lineStationId")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("crowdLevel")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("comment")));
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(json(99999, "LOW")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousCannotReport() throws Exception {
        mvc.perform(post("/api/crowd-reports").contentType(MediaType.APPLICATION_JSON).content(json(nodeId("1", "ANDHERI"), "LOW")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedReportForSameStationIsRateLimited() throws Exception {
        commuter(EMAIL);
        long node = nodeId("7", "AAREY");
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(json(node, "LOW")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/crowd-reports").with(asCommuter(EMAIL)).contentType(MediaType.APPLICATION_JSON).content(json(node, "HIGH")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void adminVerifiesOnceAndRejectedReportsAreIgnored() throws Exception {
        var user = commuter(EMAIL);
        var node = lineStationRepository.findById(nodeId("2A", "EKSAR")).orElseThrow();
        Instant now = Instant.now();
        CrowdReport report = reports.save(new CrowdReport(node, user, CrowdLevel.VERY_HIGH, null, now, now.plus(Duration.ofMinutes(15))));

        mvc.perform(patch("/api/admin/reports/{id}/verification", report.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.verifiedByName").value("MetroPulse Admin"));
        assertThat(crowdStatus.currentLevels()).doesNotContainKey(node.getId());

        mvc.perform(patch("/api/admin/reports/{id}/verification", report.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/reports/{id}/verification", report.getId()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifiedReportWinsOverNewerUnverifiedOne() {
        var user = commuter(EMAIL);
        var node = lineStationRepository.findById(nodeId("7", "KURAR")).orElseThrow();
        Instant now = Instant.now();
        CrowdReport older = new CrowdReport(node, user, CrowdLevel.HIGH, null, now.minusSeconds(300), now.plus(Duration.ofMinutes(10)));
        older.verify(VerificationStatus.APPROVED, admin(), now);
        reports.save(older);
        reports.save(new CrowdReport(node, user, CrowdLevel.LOW, null, now.minusSeconds(30), now.plus(Duration.ofMinutes(14))));
        assertThat(crowdStatus.currentLevels()).containsEntry(node.getId(), CrowdLevel.HIGH);
    }

    @Test
    void webFormShowsValidationAndAcceptsValidReport() throws Exception {
        commuter(EMAIL);
        var principal = userService.loadUserByUsername(EMAIL);
        mvc.perform(post("/reports").with(user(principal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose how crowded it is")));
        mvc.perform(post("/reports").with(user(principal)).with(csrf())
                        .param("lineStationId", "" + nodeId("2A", "OSHIWARA")).param("crowdLevel", "MODERATE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stations/" + stationId("OSHIWARA")));
    }
}
