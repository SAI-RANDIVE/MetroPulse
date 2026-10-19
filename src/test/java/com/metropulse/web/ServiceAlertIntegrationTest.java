package com.metropulse.web;

import com.metropulse.dto.form.AlertForm;
import com.metropulse.entity.StationFacility;
import com.metropulse.enums.*;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.repository.StationFacilityRepository;
import com.metropulse.service.ServiceAlertService;
import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Alert workflow, which alerts are public, and how alerts and facilities change routes. */
@Transactional
class ServiceAlertIntegrationTest extends IntegrationTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired
    ServiceAlertService alerts;
    @Autowired
    StationFacilityRepository facilities;

    private AlertForm segmentSuspension(String from, String to) {
        AlertForm form = new AlertForm();
        form.setLineId(lineRepository.findByLineCodeIgnoreCase("2A").orElseThrow().getId());
        form.setSegmentFromStationId(stationId(from));
        form.setSegmentToStationId(stationId(to));
        form.setTitle("Test suspension");
        form.setDescription("Testing");
        form.setImpact(AlertImpact.SUSPENDED);
        form.setStartsAt(LocalDateTime.now(IST).minusMinutes(5));
        return form;
    }

    private String routeUrl(String from, String to) {
        return "/api/routes?from=" + stationId(from) + "&to=" + stationId(to);
    }

    @Test
    void draftIsHiddenUntilActivatedAndThenAffectsRoutes() throws Exception {
        var draft = alerts.create(segmentSuspension("EKSAR", "BORIVALI_WEST"), admin());
        assertThat(draft.status()).isEqualTo(AlertStatus.DRAFT);
        assertThat(draft.affectedConnections()).isEqualTo(2); // both directions of one segment

        mvc.perform(get("/api/alerts/{id}", draft.id())).andExpect(status().isNotFound());
        mvc.perform(get(routeUrl("DAHISAR_EAST", "KANDIVALI_WEST")))
                .andExpect(jsonPath("$.options[0].interchanges").value(0));

        mvc.perform(patch("/api/admin/alerts/{id}", draft.id()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.verifiedAt").isNotEmpty());

        mvc.perform(get("/api/alerts")).andExpect(jsonPath("$[*].id", hasItem((int) draft.id())));
        mvc.perform(get(routeUrl("DAHISAR_EAST", "KANDIVALI_WEST")))
                .andExpect(jsonPath("$.alertsConsidered[0].id").value((int) draft.id()))
                .andExpect(jsonPath("$.options[0].interchanges").value(greaterThan(0)))
                .andExpect(jsonPath("$.options[0].explanation", containsString("suspended")));
    }

    @Test
    void invalidTransitionsAreRejected() throws Exception {
        var draft = alerts.create(segmentSuspension("EKSAR", "BORIVALI_WEST"), admin());
        mvc.perform(patch("/api/admin/alerts/{id}", draft.id()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
        alerts.transition(draft.id(), AlertStatus.ACTIVE, admin());
        alerts.transition(draft.id(), AlertStatus.RESOLVED, admin());
        mvc.perform(patch("/api/admin/alerts/{id}", draft.id()).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("final")));
        // resolved alerts stay visible as history but no longer affect routes
        mvc.perform(get("/api/alerts/{id}", draft.id())).andExpect(jsonPath("$.status").value("RESOLVED"));
        mvc.perform(get(routeUrl("DAHISAR_EAST", "KANDIVALI_WEST"))).andExpect(jsonPath("$.alertsConsidered").isEmpty());
    }

    @Test
    void futureAndInformationAlertsDoNotChangeRoutes() throws Exception {
        AlertForm future = segmentSuspension("EKSAR", "BORIVALI_WEST");
        future.setStartsAt(LocalDateTime.now(IST).plusDays(2));
        future.setPublishNow(true);
        alerts.create(future, admin());

        AlertForm info = segmentSuspension("EKSAR", "BORIVALI_WEST");
        info.setImpact(AlertImpact.INFORMATION);
        info.setPublishNow(true);
        alerts.create(info, admin());

        mvc.perform(get(routeUrl("DAHISAR_EAST", "KANDIVALI_WEST")))
                .andExpect(jsonPath("$.alertsConsidered").isEmpty())
                .andExpect(jsonPath("$.options[0].interchanges").value(0));
    }

    @Test
    void delayAlertAddsMinutesOnlyToAffectedConnections() throws Exception {
        AlertForm delay = segmentSuspension("MALAD_WEST", "LOWER_MALAD");
        delay.setImpact(AlertImpact.DELAY);
        delay.setDelayMinutes(7);
        delay.setPublishNow(true);
        alerts.create(delay, admin());
        mvc.perform(get(routeUrl("MALAD_WEST", "BANGUR_NAGAR")))
                .andExpect(jsonPath("$.options[0].delayMinutes").value(7))
                .andExpect(jsonPath("$.options[0].totalMinutes").value(4 + 7));
        mvc.perform(get(routeUrl("VERSOVA", "ANDHERI")))
                .andExpect(jsonPath("$.options[0].delayMinutes").value(0));
    }

    @Test
    void inconsistentLineAndStationIsRejected() {
        AlertForm form = segmentSuspension("EKSAR", "BORIVALI_WEST");
        form.setStationId(stationId("GHATKOPAR")); // not on Line 2A
        assertThatThrownBy(() -> alerts.create(form, admin()))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("not on Line 2A");

        AlertForm halfSegment = segmentSuspension("EKSAR", "BORIVALI_WEST");
        halfSegment.setSegmentToStationId(null);
        assertThatThrownBy(() -> alerts.create(halfSegment, admin())).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void apiValidatesAlertBody() throws Exception {
        mvc.perform(post("/api/admin/alerts").with(asAdmin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("title", "description", "startsAt")));
        String body = """
                {"lineId": %d, "title": "Closed for works", "description": "Test", "alertType": "ACCESS_RESTRICTION",
                 "severity": "CRITICAL", "impact": "SUSPENDED", "stationId": %d, "startsAt": "2026-01-01T06:00:00",
                 "publishNow": true, "status": "RESOLVED", "verifiedBy": 99}
                """.formatted(lineRepository.findByLineCodeIgnoreCase("1").orElseThrow().getId(), stationId("ANDHERI"));
        mvc.perform(post("/api/admin/alerts").with(asAdmin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE")); // server decides the status, not the client
        // a closed origin station cannot be used
        mvc.perform(get(routeUrl("ANDHERI", "GHATKOPAR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("closed")));
    }

    @Test
    void stepFreeRoutingAvoidsInterchangesWithBrokenLifts() throws Exception {
        var dnNagar = stationRepository.findByCode("DN_NAGAR").orElseThrow();
        facilities.save(new StationFacility(dnNagar, FacilityType.LIFT, FacilityStatus.OUT_OF_SERVICE));

        // Normally Andheri (W) -> Gundavali walks to D N Nagar
        mvc.perform(get(routeUrl("ANDHERI_WEST", "GUNDAVALI")))
                .andExpect(jsonPath("$.options[0].steps[0].transfer.toStation").value("D N Nagar"));
        // Step-free: the D N Nagar interchange is excluded, so the route goes round via Dahisar (East)
        mvc.perform(get(routeUrl("ANDHERI_WEST", "GUNDAVALI") + "&stepFree=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].steps[0].ride.lineCode").value("2A"))
                .andExpect(jsonPath("$.options[0].warnings", hasItem(containsString("No step-free facility data"))));
    }
}
