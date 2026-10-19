package com.metropulse.entity;

import com.metropulse.enums.LineStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "metro_line")
public class MetroLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_code", nullable = false, unique = true, length = 10)
    private String lineCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "text_color", nullable = false, length = 7)
    private String textColor;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LineStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MetroLine() {
    }

    public MetroLine(String lineCode, String name, String color, String textColor, LineStatus status) {
        this.lineCode = lineCode;
        this.name = name;
        this.color = color;
        this.textColor = textColor;
        this.status = status;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    /** Short name such as "Line 2A". */
    public String getShortName() {
        return "Line " + lineCode;
    }

    public Long getId() { return id; }
    public String getLineCode() { return lineCode; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getTextColor() { return textColor; }
    public String getOperatorName() { return operatorName; }
    public LineStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public void setStatus(LineStatus status) { this.status = status; }
}
