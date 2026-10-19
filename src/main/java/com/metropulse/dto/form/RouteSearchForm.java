package com.metropulse.dto.form;

import com.metropulse.enums.RoutePreference;
import com.metropulse.validation.DifferentStations;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Query parameters of /routes and /api/routes. */
@DifferentStations
public class RouteSearchForm {

    @NotNull(message = "Choose a starting station")
    @Positive(message = "Unknown station")
    private Long from;

    @NotNull(message = "Choose a destination station")
    @Positive(message = "Unknown station")
    private Long to;

    private RoutePreference preference = RoutePreference.FASTEST;

    private boolean stepFree;

    public Long getFrom() { return from; }
    public void setFrom(Long from) { this.from = from; }
    public Long getTo() { return to; }
    public void setTo(Long to) { this.to = to; }
    public RoutePreference getPreference() { return preference; }
    public void setPreference(RoutePreference preference) { this.preference = preference == null ? RoutePreference.FASTEST : preference; }
    public boolean isStepFree() { return stepFree; }
    public void setStepFree(boolean stepFree) { this.stepFree = stepFree; }
}
