package com.metropulse.dto.form;

import com.metropulse.enums.CrowdLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * What a commuter may send. The user, timestamps, expiry and verification status are set by the
 * server and cannot be supplied by the client (protection against over-posting).
 */
public class CrowdReportForm {

    @NotNull(message = "Choose the station and line you are at")
    @Positive(message = "Unknown station")
    private Long lineStationId;

    @NotNull(message = "Choose how crowded it is")
    private CrowdLevel crowdLevel;

    @Size(max = 280, message = "Keep the comment under 280 characters")
    private String comment;

    public Long getLineStationId() { return lineStationId; }
    public void setLineStationId(Long lineStationId) { this.lineStationId = lineStationId; }
    public CrowdLevel getCrowdLevel() { return crowdLevel; }
    public void setCrowdLevel(CrowdLevel crowdLevel) { this.crowdLevel = crowdLevel; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
