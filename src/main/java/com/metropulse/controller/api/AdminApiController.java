package com.metropulse.controller.api;

import com.metropulse.dto.*;
import com.metropulse.dto.form.AlertForm;
import com.metropulse.dto.form.AlertStatusRequest;
import com.metropulse.dto.form.FacilityForm;
import com.metropulse.dto.form.VerificationRequest;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.FacilityType;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Administrator API. Access is limited to ROLE_ADMIN by SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CrowdReportService reports;
    private final ServiceAlertService alerts;
    private final FacilityService facilities;
    private final DashboardService dashboard;
    private final UserService users;

    public AdminApiController(CrowdReportService reports, ServiceAlertService alerts, FacilityService facilities,
                              DashboardService dashboard, UserService users) {
        this.reports = reports;
        this.alerts = alerts;
        this.facilities = facilities;
        this.dashboard = dashboard;
        this.users = users;
    }

    @GetMapping("/reports")
    public PageResponse<CrowdReportView> reports(@RequestParam(required = false) VerificationStatus status,
                                                 @RequestParam(required = false) Long lineId,
                                                 @RequestParam(required = false) CrowdLevel level,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(reports.search(status, lineId, level, pageRequest(page, size)));
    }

    @PatchMapping("/reports/{id}/verification")
    public CrowdReportView verify(@PathVariable long id, @Valid @RequestBody VerificationRequest request,
                                  @AuthenticationPrincipal MetroUserPrincipal principal) {
        return reports.verify(id, request.decision(), users.require(principal.getId()));
    }

    @PostMapping("/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public AlertSummary createAlert(@Valid @RequestBody AlertForm form, @AuthenticationPrincipal MetroUserPrincipal principal) {
        return alerts.create(form, users.require(principal.getId()));
    }

    /** Workflow change: {"status": "ACTIVE" | "RESOLVED" | "CANCELLED"}. */
    @PatchMapping("/alerts/{id}")
    public AlertSummary changeAlertStatus(@PathVariable long id, @Valid @RequestBody AlertStatusRequest request,
                                          @AuthenticationPrincipal MetroUserPrincipal principal) {
        return alerts.transition(id, request.status(), users.require(principal.getId()));
    }

    @GetMapping("/dashboard/stats")
    public DashboardStats stats() {
        return dashboard.stats();
    }

    @GetMapping("/facilities")
    public PageResponse<FacilityView> facilities(@RequestParam(required = false) Long stationId,
                                                 @RequestParam(required = false) FacilityType type,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(facilities.search(stationId, type,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("station.name", "facilityType"))));
    }

    @PostMapping("/facilities")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityView createFacility(@Valid @RequestBody FacilityForm form, @AuthenticationPrincipal MetroUserPrincipal principal) {
        return facilities.create(form, users.require(principal.getId()));
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }
}
