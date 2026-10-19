package com.metropulse.controller.web.admin;

import com.metropulse.dto.CrowdReportView;
import com.metropulse.enums.VerificationStatus;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final DashboardService dashboard;
    private final CrowdReportService reports;
    private final AuditService audit;
    private final DemoDataService demoData;
    private final UserService users;

    public AdminDashboardController(DashboardService dashboard, CrowdReportService reports, AuditService audit,
                                    DemoDataService demoData, UserService users) {
        this.dashboard = dashboard;
        this.reports = reports;
        this.audit = audit;
        this.demoData = demoData;
        this.users = users;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<CrowdReportView> pending = reports.search(VerificationStatus.PENDING, null, null, PageRequest.of(0, 5)).getContent();
        model.addAttribute("stats", dashboard.stats());
        model.addAttribute("pending", pending);
        model.addAttribute("recent", reports.search(null, null, null, PageRequest.of(0, 6)).getContent());
        model.addAttribute("audit", audit.latest());
        model.addAttribute("demoResetAllowed", demoData.resetAllowed());
        return "admin/dashboard";
    }

    @GetMapping("/audit")
    public String auditLog(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", audit.page(PageRequest.of(Math.max(page, 0), 25)));
        return "admin/audit";
    }

    @PostMapping("/demo/reset")
    public String resetDemo(@AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        try {
            demoData.reset(users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Demonstration data was reset. Only records flagged as demo were replaced.");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}
