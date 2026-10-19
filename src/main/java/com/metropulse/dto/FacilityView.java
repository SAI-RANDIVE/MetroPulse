package com.metropulse.dto;

import com.metropulse.entity.StationFacility;
import com.metropulse.enums.FacilityStatus;
import com.metropulse.enums.FacilityType;

import java.time.Instant;

public record FacilityView(long id, long stationId, String stationName, FacilityType type, FacilityStatus status,
                           String description, String source, Instant lastVerifiedAt, boolean demo) {

    public static FacilityView of(StationFacility f) {
        return new FacilityView(f.getId(), f.getStation().getId(), f.getStation().getName(), f.getFacilityType(),
                f.getStatus(), f.getDescription(), f.getSource(), f.getLastVerifiedAt(), f.isDemo());
    }
}
