package com.metropulse.controller.web.admin;

import com.metropulse.dto.form.FacilityForm;
import com.metropulse.enums.FacilityStatus;
import com.metropulse.enums.FacilityType;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.FacilityService;
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

@Controller
@RequestMapping("/admin/facilities")
public class AdminFacilityController {

    private final FacilityService facilities;
    private final StationService stations;
    private final UserService users;

    public AdminFacilityController(FacilityService facilities, StationService stations, UserService users) {
        this.facilities = facilities;
        this.stations = stations;
        this.users = users;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long stationId, @RequestParam(required = false) FacilityType type,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        if (!model.containsAttribute("form")) {
            FacilityForm form = new FacilityForm();
            form.setStationId(stationId);
            model.addAttribute("form", form);
        }
        return page(model, stationId, type, page);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") FacilityForm form, BindingResult binding,
                         @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return page(model, null, null, 0);
        }
        try {
            var saved = facilities.create(form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", saved.type().getLabel() + " added at " + saved.stationName() + ".");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/facilities";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @RequestParam FacilityStatus status,
                         @RequestParam(required = false) String description, @RequestParam(required = false) String source,
                         @AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        if ((description != null && description.length() > 300) || (source != null && source.length() > 200)) {
            redirect.addFlashAttribute("error", "Description (300) or source (200 characters) is too long.");
            return "redirect:/admin/facilities";
        }
        try {
            var saved = facilities.update(id, status, description, source, users.require(principal.getId()));
            redirect.addFlashAttribute("success", saved.type().getLabel() + " at " + saved.stationName()
                    + " marked " + saved.status().getLabel().toLowerCase() + " and verified now.");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/facilities";
    }

    private String page(Model model, Long stationId, FacilityType type, int page) {
        model.addAttribute("page", facilities.search(stationId, type,
                PageRequest.of(Math.max(page, 0), 15, Sort.by("station.name", "facilityType"))));
        model.addAttribute("stationId", stationId);
        model.addAttribute("type", type);
        model.addAttribute("stations", stations.listAll());
        model.addAttribute("types", FacilityType.values());
        model.addAttribute("statuses", FacilityStatus.values());
        return "admin/facilities";
    }
}
