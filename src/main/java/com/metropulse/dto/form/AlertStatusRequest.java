package com.metropulse.dto.form;

import com.metropulse.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /api/admin/alerts/{id}: moves an alert through its workflow. */
public record AlertStatusRequest(@NotNull(message = "status is required") AlertStatus status) {
}
