package com.metropulse.entity;

import com.metropulse.enums.AuditAction;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Column(name = "actor_email", length = 160)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(AppUser actor, AuditAction action, String entityType, Long entityId, String details, Instant createdAt) {
        this.actor = actor;
        this.actorEmail = actor == null ? "system" : actor.getEmail();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details == null || details.length() <= 500 ? details : details.substring(0, 500);
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getActorEmail() { return actorEmail; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
