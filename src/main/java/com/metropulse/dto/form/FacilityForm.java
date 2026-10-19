package com.metropulse.dto.form;

import com.metropulse.enums.FacilityStatus;
import com.metropulse.enums.FacilityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FacilityForm {

    @NotNull(message = "Choose a station")
    private Long stationId;

    @NotNull(message = "Choose a facility type")
    private FacilityType facilityType;

    @NotNull(message = "Choose a status")
    private FacilityStatus status = FacilityStatus.AVAILABLE;

    @Size(max = 300, message = "Description is too long")
    private String description;

    @Size(max = 200, message = "Source is too long")
    private String source;

    public Long getStationId() { return stationId; }
    public void setStationId(Long stationId) { this.stationId = stationId; }
    public FacilityType getFacilityType() { return facilityType; }
    public void setFacilityType(FacilityType facilityType) { this.facilityType = facilityType; }
    public FacilityStatus getStatus() { return status; }
    public void setStatus(FacilityStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
