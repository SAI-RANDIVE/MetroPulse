package com.metropulse.entity;

import jakarta.persistence.*;

/** A station's membership of one line - one node of the routing graph. */
@Entity
@Table(name = "line_station")
public class LineStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private MetroLine line;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    /** false = trains pass through without stopping (no boarding, alighting or changing). */
    @Column(name = "is_operational", nullable = false)
    private boolean operational = true;

    protected LineStation() {
    }

    public LineStation(MetroLine line, Station station, int sequenceNo) {
        this.line = line;
        this.station = station;
        this.sequenceNo = sequenceNo;
    }

    public Long getId() { return id; }
    public MetroLine getLine() { return line; }
    public Station getStation() { return station; }
    public int getSequenceNo() { return sequenceNo; }
    public boolean isOperational() { return operational; }

    public void setOperational(boolean operational) { this.operational = operational; }
}
