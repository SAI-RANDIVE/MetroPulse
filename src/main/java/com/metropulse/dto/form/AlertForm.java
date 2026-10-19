package com.metropulse.dto.form;

import com.metropulse.enums.AlertImpact;
import com.metropulse.enums.AlertSeverity;
import com.metropulse.enums.AlertType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Create/edit form for service alerts (also the JSON body of POST /api/admin/alerts).
 * Times are local Mumbai time. Status, creator and verifier are controlled by the server.
 * <p>
 * To target a line segment, give the line plus segment start and end stations: every ride
 * connection between them (both directions) becomes an affected connection.
 */
public class AlertForm {

    private Long lineId;
    private Long stationId;
    private Long segmentFromStationId;
    private Long segmentToStationId;

    @NotBlank(message = "Give the alert a title")
    @Size(max = 140, message = "Title is too long")
    private String title;

    @NotBlank(message = "Describe what passengers should know")
    @Size(max = 2000, message = "Description is too long")
    private String description;

    @NotNull(message = "Choose a category")
    private AlertType alertType = AlertType.SERVICE_DISRUPTION;

    @NotNull(message = "Choose a severity")
    private AlertSeverity severity = AlertSeverity.WARNING;

    @NotNull(message = "Choose how the alert affects routes")
    private AlertImpact impact = AlertImpact.INFORMATION;

    @Min(value = 0, message = "Delay cannot be negative")
    @Max(value = 180, message = "Delay must be at most 180 minutes")
    private int delayMinutes;

    @NotNull(message = "Choose a start time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startsAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endsAt;

    /** Create directly as ACTIVE (verified by the creating admin) instead of DRAFT. */
    private boolean publishNow;

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }
    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public Long getSegmentFromStationId() { return segmentFromStationId; }
    public void setSegmentFromStationId(Long segmentFromStationId) { this.segmentFromStationId = segmentFromStationId; }
    public Long getSegmentToStationId() { return segmentToStationId; }
    public void setSegmentToStationId(Long segmentToStationId) { this.segmentToStationId = segmentToStationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public AlertImpact getImpact() { return impact; }
    public void setImpact(AlertImpact impact) { this.impact = impact; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public boolean isPublishNow() { return publishNow; }
    public void setPublishNow(boolean publishNow) { this.publishNow = publishNow; }
}
