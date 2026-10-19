package com.metropulse.controller.web;

import com.metropulse.dto.form.RouteSearchForm;
import com.metropulse.enums.RoutePreference;
import com.metropulse.exception.InvalidRequestException;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.exception.RouteNotFoundException;
import com.metropulse.service.LineService;
import com.metropulse.service.RoutePlannerService;
import com.metropulse.service.ServiceAlertService;
import com.metropulse.service.StationService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Landing page, journey planner and route results. */
@Controller
public class HomeController {

    private final StationService stations;
    private final LineService lines;
    private final ServiceAlertService alerts;
    private final RoutePlannerService planner;

    public HomeController(StationService stations, LineService lines, ServiceAlertService alerts, RoutePlannerService planner) {
        this.stations = stations;
        this.lines = lines;
        this.alerts = alerts;
        this.planner = planner;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("form", new RouteSearchForm());
        addPlannerData(model);
        model.addAttribute("lines", lines.listAll());
        model.addAttribute("alerts", alerts.publicActive());
        model.addAttribute("interchanges", stations.interchanges());
        return "index";
    }

    @GetMapping("/plan")
    public String plan(@ModelAttribute("form") RouteSearchForm form, Model model) {
        addPlannerData(model);
        return "routes/plan";
    }

    @GetMapping("/routes")
    public String routes(@Valid @ModelAttribute("form") RouteSearchForm form, BindingResult binding, Model model,
                         HttpServletResponse response) {
        addPlannerData(model);
        if (binding.hasErrors()) {
            response.setStatus(400);
            return "routes/plan";
        }
        try {
            model.addAttribute("result", planner.plan(form));
            return "routes/results";
        } catch (ResourceNotFoundException e) {
            response.setStatus(404);
            binding.reject("unknownStation", "One of the chosen stations does not exist. Please pick again.");
        } catch (RouteNotFoundException | InvalidRequestException e) {
            response.setStatus(e.getStatus().value());
            binding.reject("noRoute", e.getMessage());
        }
        return "routes/plan";
    }

    private void addPlannerData(Model model) {
        model.addAttribute("stations", stations.listAll());
        model.addAttribute("preferences", RoutePreference.values());
    }
}
