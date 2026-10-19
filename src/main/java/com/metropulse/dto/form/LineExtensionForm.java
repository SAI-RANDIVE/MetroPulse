package com.metropulse.dto.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Adds an existing station to one end of a line, creating ride edges in both directions. */
public class LineExtensionForm {

    public enum End { START, END }

    @NotNull(message = "Choose a station")
    private Long stationId;

    @NotNull(message = "Choose which end of the line")
    private End end = End.END;

    @Min(value = 1, message = "Travel time must be at least 1 minute")
    @Max(value = 30, message = "Travel time must be at most 30 minutes")
    private int minutes = 2;

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public End getEnd() { return end; }
    public void setEnd(End end) { this.end = end; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
}
