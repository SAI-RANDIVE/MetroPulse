package com.metropulse.controller.web;

import com.metropulse.config.TemplateFormatter;
import com.metropulse.dto.form.CrowdReportForm;
import com.metropulse.enums.CrowdLevel;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.CrowdReportService;
import com.metropulse.service.StationService;
import com.metropulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Crowd-report submission page (signed-in users only, see SecurityConfig). */
@Controller
@RequestMapping("/reports")
public class CrowdReportController {

    private final CrowdReportService reports;
    private final StationService stations;
    private final UserService users;
    private final TemplateFormatter fmt;

    public CrowdReportController(CrowdReportService reports, StationService stations, UserService users, TemplateFormatter fmt) {
        this.reports = reports;
        this.stations = stations;
        this.users = users;
        this.fmt = fmt;
    }

    @GetMapping("/new")
    public String form(@RequestParam(required = false) Long lineStationId, @RequestParam(required = false) Long stationId,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal MetroUserPrincipal principal, Model model) {
        CrowdReportForm form = new CrowdReportForm();
        form.setLineStationId(lineStationId);
        if (lineStationId == null && stationId != null) {
            stations.allNodes().stream().filter(n -> n.getStation().getId().equals(stationId)).findFirst()
                    .ifPresent(n -> form.setLineStationId(n.getId()));
        }
        model.addAttribute("form", form);
        return page(model, principal, page);
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") CrowdReportForm form, BindingResult binding,
                         @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return page(model, principal, 0);
        }
        try {
            var saved = reports.submit(form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Thanks! Your report for " + saved.stationName() + " (" + saved.line().code()
                    + ") counts until " + fmt.time(saved.expiresAt()) + (principal.isAdmin() ? " and marked as verified." : " and awaits moderation."));
            return "redirect:/stations/" + saved.stationId();
        } catch (MetroPulseException e) {
            binding.reject("rejected", e.getMessage());
            return page(model, principal, 0);
        }
    }

    private String page(Model model, MetroUserPrincipal principal, int page) {
        model.addAttribute("nodes", stations.allNodes());
        model.addAttribute("levels", CrowdLevel.values());
        model.addAttribute("mine", reports.mine(principal.getId(), page));
        return "reports/new";
    }
}
