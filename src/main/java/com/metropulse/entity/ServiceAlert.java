package com.metropulse.entity;

import com.metropulse.enums.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "service_alert")
public class ServiceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private MetroLine line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertImpact impact = AlertImpact.INFORMATION;

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status = AlertStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private AppUser verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    /** Connections this alert applies to (e.g. both directions of a suspended segment). */
    @ManyToMany
    @JoinTable(name = "alert_affected_edge",
            joinColumns = @JoinColumn(name = "alert_id"),
            inverseJoinColumns = @JoinColumn(name = "edge_id"))
    private Set<NetworkEdge> affectedEdges = new LinkedHashSet<>();

    public ServiceAlert() {
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** True when commuters should see this alert and routing should apply it. */
    public boolean isInEffectAt(Instant now) {
        return status == AlertStatus.ACTIVE
                && verifiedAt != null
                && !startsAt.isAfter(now)
                && (endsAt == null || endsAt.isAfter(now));
    }

    public Long getId() { return id; }
    public MetroLine getLine() { return line; }
    public Station getStation() { return station; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AlertType getAlertType() { return alertType; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertImpact getImpact() { return impact; }
    public int getDelayMinutes() { return delayMinutes; }
    public AlertStatus getStatus() { return status; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public AppUser getCreatedBy() { return createdBy; }
    public AppUser getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isDemo() { return demo; }
    public Set<NetworkEdge> getAffectedEdges() { return affectedEdges; }

    public void setLine(MetroLine line) { this.line = line; }
    public void setStation(Station station) { this.station = station; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public void setImpact(AlertImpact impact) { this.impact = impact; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }
    public void setVerifiedBy(AppUser verifiedBy) { this.verifiedBy = verifiedBy; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
