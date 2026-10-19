package com.metropulse.entity;

import com.metropulse.enums.CrowdLevel;
import com.metropulse.enums.VerificationStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "crowd_report")
public class CrowdReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_station_id", nullable = false)
    private LineStation lineStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "crowd_level", nullable = false, length = 20)
    private CrowdLevel crowdLevel;

    @Column(length = 280)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private AppUser verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    protected CrowdReport() {
    }

    public CrowdReport(LineStation lineStation, AppUser user, CrowdLevel crowdLevel, String comment,
                       Instant createdAt, Instant expiresAt) {
        this.lineStation = lineStation;
        this.user = user;
        this.crowdLevel = crowdLevel;
        this.comment = comment;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void verify(VerificationStatus decision, AppUser admin, Instant when) {
        this.verificationStatus = decision;
        this.verifiedBy = admin;
        this.verifiedAt = when;
    }

    public Long getId() { return id; }
    public LineStation getLineStation() { return lineStation; }
    public AppUser getUser() { return user; }
    public CrowdLevel getCrowdLevel() { return crowdLevel; }
    public String getComment() { return comment; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public AppUser getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public boolean isDemo() { return demo; }

    public void setDemo(boolean demo) { this.demo = demo; }
}
