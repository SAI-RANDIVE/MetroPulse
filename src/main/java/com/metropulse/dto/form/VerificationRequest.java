package com.metropulse.dto.form;

import com.metropulse.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PATCH /api/admin/reports/{id}/verification. The verifying admin comes from the login, never the body. */
public record VerificationRequest(@NotNull(message = "decision is required (APPROVED or REJECTED)") VerificationStatus decision) {
}
