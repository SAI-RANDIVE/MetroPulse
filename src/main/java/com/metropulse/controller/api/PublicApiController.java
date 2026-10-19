package com.metropulse.controller.api;

import com.metropulse.dto.AlertSummary;
import com.metropulse.dto.LineSummary;
import com.metropulse.dto.StationStatusView;
import com.metropulse.dto.StationSummary;
import com.metropulse.dto.form.RouteSearchForm;
import com.metropulse.dto.route.RoutePlanResult;
import com.metropulse.service.LineService;
import com.metropulse.service.RoutePlannerService;
import com.metropulse.service.ServiceAlertService;
import com.metropulse.service.StationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only endpoints anyone may call: network data, routes and verified alerts. */
@RestController
@RequestMapping("/api")
public class PublicApiController {

    private final LineService lines;
    private final StationService stations;
    private final RoutePlannerService planner;
    private final ServiceAlertService alerts;

    public PublicApiController(LineService lines, StationService stations, RoutePlannerService planner, ServiceAlertService alerts) {
        this.lines = lines;
        this.stations = stations;
        this.planner = planner;
        this.alerts = alerts;
    }

    @GetMapping("/lines")
    public List<LineSummary> lines() {
        return lines.listAll();
    }

    /** All stations, or a partial-name search when {@code query} is given (used by the autocomplete). */
    @GetMapping("/stations")
    public List<StationSummary> stations(@RequestParam(required = false) String query) {
        return query == null ? stations.listAll() : stations.search(query);
    }

    @GetMapping("/stations/{id}")
    public StationSummary station(@PathVariable long id) {
        return stations.summary(id);
    }

    @GetMapping("/stations/{id}/status")
    public StationStatusView stationStatus(@PathVariable long id) {
        return stations.status(id);
    }

    /** Example: /api/routes?from=29&to=33&preference=LESS_CROWDED&stepFree=false */
    @GetMapping("/routes")
    public RoutePlanResult route(@Valid @ModelAttribute RouteSearchForm form) {
        return planner.plan(form);
    }

    @GetMapping("/alerts")
    public List<AlertSummary> alerts() {
        return alerts.publicActive();
    }

    @GetMapping("/alerts/{id}")
    public AlertSummary alert(@PathVariable long id) {
        return alerts.publicGet(id);
    }
}
