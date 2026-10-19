package com.metropulse.controller.api;

import com.metropulse.dto.CrowdReportView;
import com.metropulse.dto.PageResponse;
import com.metropulse.dto.form.CrowdReportForm;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.CrowdReportService;
import com.metropulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Signed-in commuters: submit crowd reports and list their own. */
@RestController
@RequestMapping("/api/crowd-reports")
public class CrowdReportApiController {

    private final CrowdReportService reports;
    private final UserService users;

    public CrowdReportApiController(CrowdReportService reports, UserService users) {
        this.reports = reports;
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrowdReportView submit(@Valid @RequestBody CrowdReportForm form, @AuthenticationPrincipal MetroUserPrincipal principal) {
        return reports.submit(form, users.require(principal.getId()));
    }

    @GetMapping("/mine")
    public PageResponse<CrowdReportView> mine(@RequestParam(defaultValue = "0") int page,
                                              @AuthenticationPrincipal MetroUserPrincipal principal) {
        return PageResponse.of(reports.mine(principal.getId(), page));
    }
}
