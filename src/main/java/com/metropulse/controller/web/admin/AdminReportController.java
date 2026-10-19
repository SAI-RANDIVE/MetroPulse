package com.metropulse.controller.web.admin;

import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.CrowdReportService;
import com.metropulse.service.LineService;
import com.metropulse.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Crowd-report moderation with server-side filters and pagination. */
@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final CrowdReportService reports;
    private final LineService lines;
    private final UserService users;

    public AdminReportController(CrowdReportService reports, LineService lines, UserService users) {
        this.reports = reports;
        this.lines = lines;
        this.users = users;
    }

    @GetMapping
    public String list(@RequestParam(required = false) VerificationStatus status,
                       @RequestParam(required = false) Long lineId,
                       @RequestParam(required = false) CrowdLevel level,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", reports.search(status, lineId, level, PageRequest.of(Math.max(page, 0), 15)));
        model.addAttribute("status", status);
        model.addAttribute("lineId", lineId);
        model.addAttribute("level", level);
        model.addAttribute("lines", lines.badges());
        model.addAttribute("statuses", VerificationStatus.values());
        model.addAttribute("levels", CrowdLevel.values());
        return "admin/reports";
    }

    @PostMapping("/{id}/verification")
    public String verify(@PathVariable long id, @RequestParam VerificationStatus decision,
                         @RequestParam(required = false) String back,
                         @AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        try {
            var report = reports.verify(id, decision, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Report #" + id + " (" + report.stationName() + ") "
                    + (decision == VerificationStatus.APPROVED ? "approved." : "rejected."));
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        // only allow returning to our own admin pages
        return "redirect:" + (back != null && back.startsWith("/admin") && !back.startsWith("//") ? back : "/admin/reports");
    }
}
