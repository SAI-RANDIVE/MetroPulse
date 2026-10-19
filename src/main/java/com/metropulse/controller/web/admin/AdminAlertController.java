package com.metropulse.controller.web.admin;

import com.metropulse.config.MetroPulseProperties;
import com.metropulse.dto.form.AlertForm;
import com.metropulse.enums.*;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.LineService;
import com.metropulse.service.ServiceAlertService;
import com.metropulse.service.StationService;
import com.metropulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Alert management: list, create, edit and move through DRAFT -> ACTIVE -> RESOLVED / CANCELLED. */
@Controller
@RequestMapping("/admin/alerts")
public class AdminAlertController {

    private final ServiceAlertService alerts;
    private final LineService lines;
    private final StationService stations;
    private final UserService users;
    private final Clock clock;
    private final MetroPulseProperties properties;

    public AdminAlertController(ServiceAlertService alerts, LineService lines, StationService stations, UserService users,
                                Clock clock, MetroPulseProperties properties) {
        this.alerts = alerts;
        this.lines = lines;
        this.stations = stations;
        this.users = users;
        this.clock = clock;
        this.properties = properties;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AlertStatus status, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", alerts.adminPage(status, PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "updatedAt"))));
        model.addAttribute("status", status);
        model.addAttribute("statuses", AlertStatus.values());
        return "admin/alerts";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        AlertForm form = new AlertForm();
        form.setStartsAt(LocalDateTime.now(clock.withZone(properties.timeZone())).truncatedTo(ChronoUnit.MINUTES));
        model.addAttribute("form", form);
        return formPage(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AlertForm form, BindingResult binding,
                         @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return formPage(model, null);
        }
        try {
            var saved = alerts.create(form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Alert \"" + saved.title() + "\" saved as " + saved.status() + ".");
            return "redirect:/admin/alerts";
        } catch (MetroPulseException e) {
            binding.reject("invalid", e.getMessage());
            return formPage(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("form", alerts.toForm(id));
        return formPage(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") AlertForm form, BindingResult binding,
                         @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return formPage(model, id);
        }
        try {
            alerts.update(id, form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Alert updated.");
            return "redirect:/admin/alerts";
        } catch (MetroPulseException e) {
            binding.reject("invalid", e.getMessage());
            return formPage(model, id);
        }
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable long id, @RequestParam AlertStatus target,
                               @AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        try {
            var alert = alerts.transition(id, target, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "\"" + alert.title() + "\" is now " + alert.status() + ".");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/alerts";
    }

    private String formPage(Model model, Long alertId) {
        model.addAttribute("alertId", alertId);
        if (alertId != null) {
            model.addAttribute("alert", alerts.adminGet(alertId));
        }
        model.addAttribute("lines", lines.badges());
        model.addAttribute("stations", stations.listAll());
        model.addAttribute("types", AlertType.values());
        model.addAttribute("severities", AlertSeverity.values());
        model.addAttribute("impacts", AlertImpact.values());
        return "admin/alert-form";
    }
}
