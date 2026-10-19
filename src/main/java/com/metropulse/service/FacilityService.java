package com.metropulse.service;

import com.metropulse.dto.FacilityView;
import com.metropulse.dto.form.FacilityForm;
import com.metropulse.entity.AppUser;
import com.metropulse.entity.Station;
import com.metropulse.entity.StationFacility;
import com.metropulse.enums.AccessibilityState;
import com.metropulse.enums.AuditAction;
import com.metropulse.enums.FacilityStatus;
import com.metropulse.enums.FacilityType;
import com.metropulse.exception.ResourceNotFoundException;
import com.metropulse.repository.StationFacilityRepository;
import com.metropulse.repository.StationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FacilityService {

    private final StationFacilityRepository facilities;
    private final StationRepository stations;
    private final AuditService audit;
    private final Clock clock;

    public FacilityService(StationFacilityRepository facilities, StationRepository stations, AuditService audit, Clock clock) {
        this.facilities = facilities;
        this.stations = stations;
        this.audit = audit;
        this.clock = clock;
    }

    public List<FacilityView> forStation(long stationId) {
        return facilities.findByStationIdOrderByFacilityType(stationId).stream().map(FacilityView::of).toList();
    }

    public AccessibilityState accessibilityOf(long stationId) {
        return evaluate(facilities.findByStationIdOrderByFacilityType(stationId));
    }

    /**
     * Step-free rule: a station is STEP_FREE_RECORDED when at least one lift or wheelchair-access
     * facility is AVAILABLE; NOT_STEP_FREE when such facilities are recorded but none is available
     * (at least one out of service); UNKNOWN when nothing useful is recorded. One recorded lift is not
     * a claim that the whole station is accessible - the UI says "recorded", never "fully accessible".
     */
    public static AccessibilityState evaluate(List<StationFacility> stationFacilities) {
        List<StationFacility> stepFree = stationFacilities.stream()
                .filter(f -> f.getFacilityType().isStepFree()).toList();
        if (stepFree.stream().anyMatch(f -> f.getStatus() == FacilityStatus.AVAILABLE)) {
            return AccessibilityState.STEP_FREE_RECORDED;
        }
        if (stepFree.stream().anyMatch(f -> f.getStatus() == FacilityStatus.OUT_OF_SERVICE)) {
            return AccessibilityState.NOT_STEP_FREE;
        }
        return AccessibilityState.UNKNOWN;
    }

    /** Accessibility state for every station (stations without records are UNKNOWN). */
    public Map<Long, AccessibilityState> accessibilityByStation() {
        return facilities.findAllWithStation().stream()
                .collect(Collectors.groupingBy(f -> f.getStation().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> evaluate(e.getValue())));
    }

    public Set<Long> notStepFreeStationIds() {
        return accessibilityByStation().entrySet().stream()
                .filter(e -> e.getValue() == AccessibilityState.NOT_STEP_FREE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Page<FacilityView> search(Long stationId, FacilityType type, Pageable pageable) {
        return facilities.search(stationId, type, pageable).map(FacilityView::of);
    }

    @Transactional
    public FacilityView create(FacilityForm form, AppUser admin) {
        Station station = stations.findById(form.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Station", form.getStationId()));
        StationFacility facility = new StationFacility(station, form.getFacilityType(), form.getStatus());
        apply(facility, form);
        facilities.save(facility);
        audit.record(admin, AuditAction.FACILITY_CREATED, "StationFacility", facility.getId(),
                form.getFacilityType() + " at " + station.getName() + " -> " + form.getStatus());
        return FacilityView.of(facility);
    }

    /** Updates status/description and marks the record as verified now by this administrator. */
    @Transactional
    public FacilityView update(long facilityId, FacilityStatus status, String description, String source, AppUser admin) {
        StationFacility facility = facilities.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility", facilityId));
        facility.setStatus(status);
        facility.setDescription(blankToNull(description));
        facility.setSource(blankToNull(source));
        facility.setLastVerifiedAt(clock.instant());
        facility.setDemo(false); // an administrator has now checked this record
        audit.record(admin, AuditAction.FACILITY_UPDATED, "StationFacility", facilityId,
                facility.getFacilityType() + " at " + facility.getStation().getName() + " -> " + status);
        return FacilityView.of(facility);
    }

    private void apply(StationFacility facility, FacilityForm form) {
        facility.setDescription(blankToNull(form.getDescription()));
        facility.setSource(blankToNull(form.getSource()));
        facility.setLastVerifiedAt(clock.instant());
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
