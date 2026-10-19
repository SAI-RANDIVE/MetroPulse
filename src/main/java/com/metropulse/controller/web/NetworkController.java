package com.metropulse.controller.web;

import com.metropulse.service.LineService;
import com.metropulse.service.ServiceAlertService;
import com.metropulse.service.StationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Public station directory, station pages, line pages and the alerts page. */
@Controller
public class NetworkController {

    private final StationService stations;
    private final LineService lines;
    private final ServiceAlertService alerts;

    public NetworkController(StationService stations, LineService lines, ServiceAlertService alerts) {
        this.stations = stations;
        this.lines = lines;
        this.alerts = alerts;
    }

    @GetMapping("/stations")
    public String directory(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q == null ? "" : q.strip());
        model.addAttribute("results", stations.search(q));
        model.addAttribute("lines", lines.listAll());
        return "stations/list";
    }

    @GetMapping("/stations/{id}")
    public String station(@PathVariable long id, Model model) {
        model.addAttribute("detail", stations.detail(id));
        return "stations/detail";
    }

    @GetMapping("/lines/{code}")
    public String line(@PathVariable String code, Model model) {
        model.addAttribute("line", lines.byCode(code));
        model.addAttribute("allLines", lines.badges());
        return "lines/detail";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("active", alerts.publicActive());
        model.addAttribute("resolved", alerts.recentlyResolved());
        return "alerts/list";
    }
}
