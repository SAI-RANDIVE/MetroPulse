package com.metropulse.entity;

import com.metropulse.enums.FacilityStatus;
import com.metropulse.enums.FacilityType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "station_facility")
public class StationFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Enumerated(EnumType.STRING)
    @Column(name = "facility_type", nullable = false, length = 30)
    private FacilityType facilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacilityStatus status;

    @Column(length = 300)
    private String description;

    @Column(length = 200)
    private String source;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    protected StationFacility() {
    }

    public StationFacility(Station station, FacilityType facilityType, FacilityStatus status) {
        this.station = station;
        this.facilityType = facilityType;
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Station getStation() { return station; }
    public FacilityType getFacilityType() { return facilityType; }
    public FacilityStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isDemo() { return demo; }

    public void setStatus(FacilityStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setSource(String source) { this.source = source; }
    public void setLastVerifiedAt(Instant lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
