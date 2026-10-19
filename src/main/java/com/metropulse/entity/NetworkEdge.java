package com.metropulse.entity;

import com.metropulse.enums.EdgeType;
import jakarta.persistence.*;

import java.math.BigDecimal;

/** A directed connection between two line-specific nodes. */
@Entity
@Table(name = "network_edge")
public class NetworkEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private LineStation fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private LineStation toNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "edge_type", nullable = false, length = 10)
    private EdgeType edgeType;

    @Column(name = "base_minutes", nullable = false)
    private int baseMinutes;

    @Column(name = "distance_km", precision = 6, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(length = 200)
    private String note;

    protected NetworkEdge() {
    }

    public NetworkEdge(LineStation fromNode, LineStation toNode, EdgeType edgeType, int baseMinutes) {
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.edgeType = edgeType;
        this.baseMinutes = baseMinutes;
    }

    public Long getId() { return id; }
    public LineStation getFromNode() { return fromNode; }
    public LineStation getToNode() { return toNode; }
    public EdgeType getEdgeType() { return edgeType; }
    public int getBaseMinutes() { return baseMinutes; }
    public BigDecimal getDistanceKm() { return distanceKm; }
    public boolean isActive() { return active; }
    public String getNote() { return note; }

    public void setBaseMinutes(int baseMinutes) { this.baseMinutes = baseMinutes; }
    public void setActive(boolean active) { this.active = active; }
    public void setNote(String note) { this.note = note; }
}
