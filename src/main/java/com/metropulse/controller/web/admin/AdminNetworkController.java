package com.metropulse.controller.web.admin;

import com.metropulse.dto.form.LineExtensionForm;
import com.metropulse.dto.form.StationForm;
import com.metropulse.enums.StationStatus;
import com.metropulse.exception.MetroPulseException;
import com.metropulse.security.MetroUserPrincipal;
import com.metropulse.service.LineService;
import com.metropulse.service.StationService;
import com.metropulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Station records, line-to-station mappings and connection times. */
@Controller
@RequestMapping("/admin")
public class AdminNetworkController {

    private final StationService stations;
    private final LineService lines;
    private final UserService users;

    public AdminNetworkController(StationService stations, LineService lines, UserService users) {
        this.stations = stations;
        this.lines = lines;
        this.users = users;
    }

    @GetMapping("/stations")
    public String stations(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("stations", stations.search(q));
        model.addAttribute("lines", lines.listAll());
        return "admin/stations";
    }

    @GetMapping("/stations/new")
    public String newStation(Model model) {
        model.addAttribute("form", new StationForm());
        return stationForm(model, null);
    }

    @PostMapping("/stations")
    public String createStation(@Valid @ModelAttribute("form") StationForm form, BindingResult binding,
                                @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return stationForm(model, null);
        }
        try {
            var station = stations.create(form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", station.getName() + " created. Add it to a line from the line page.");
            return "redirect:/admin/stations";
        } catch (MetroPulseException e) {
            binding.reject("invalid", e.getMessage());
            return stationForm(model, null);
        }
    }

    @GetMapping("/stations/{id}/edit")
    public String editStation(@PathVariable long id, Model model) {
        model.addAttribute("form", stations.toForm(id));
        return stationForm(model, id);
    }

    @PostMapping("/stations/{id}")
    public String updateStation(@PathVariable long id, @Valid @ModelAttribute("form") StationForm form, BindingResult binding,
                                @AuthenticationPrincipal MetroUserPrincipal principal, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return stationForm(model, id);
        }
        try {
            stations.update(id, form, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Station updated.");
            return "redirect:/admin/stations";
        } catch (MetroPulseException e) {
            binding.reject("invalid", e.getMessage());
            return stationForm(model, id);
        }
    }

    @GetMapping("/lines/{id}")
    public String line(@PathVariable long id, Model model) {
        var line = lines.require(id);
        model.addAttribute("line", lines.byCode(line.getLineCode()));
        model.addAttribute("edges", lines.rideEdges(id));
        model.addAttribute("stations", stations.listAll());
        if (!model.containsAttribute("extension")) {
            model.addAttribute("extension", new LineExtensionForm());
        }
        return "admin/line";
    }

    @PostMapping("/lines/{id}/extend")
    public String extend(@PathVariable long id, @Valid @ModelAttribute("extension") LineExtensionForm form, BindingResult binding,
                         @AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", binding.getAllErrors().getFirst().getDefaultMessage());
        } else {
            try {
                lines.extend(id, form, users.require(principal.getId()));
                redirect.addFlashAttribute("success", "Station added and connected in both directions.");
            } catch (MetroPulseException e) {
                redirect.addFlashAttribute("error", e.getMessage());
            }
        }
        return "redirect:/admin/lines/" + id;
    }

    @PostMapping("/line-stations/{id}/operational")
    public String setOperational(@PathVariable long id, @RequestParam boolean operational, @RequestParam long lineId,
                                 @AuthenticationPrincipal MetroUserPrincipal principal, RedirectAttributes redirect) {
        try {
            lines.setOperational(id, operational, users.require(principal.getId()));
            redirect.addFlashAttribute("success", operational ? "Trains stop here again." : "Trains now pass through without stopping.");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/lines/" + lineId;
    }

    @PostMapping("/edges/{id}")
    public String updateEdge(@PathVariable long id, @RequestParam int minutes, @RequestParam(defaultValue = "false") boolean active,
                             @RequestParam long lineId, @AuthenticationPrincipal MetroUserPrincipal principal,
                             RedirectAttributes redirect) {
        try {
            lines.updateEdge(id, minutes, active, users.require(principal.getId()));
            redirect.addFlashAttribute("success", "Connection updated.");
        } catch (MetroPulseException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/lines/" + lineId;
    }

    private String stationForm(Model model, Long stationId) {
        model.addAttribute("stationId", stationId);
        model.addAttribute("statuses", StationStatus.values());
        return "admin/station-form";
    }
}
