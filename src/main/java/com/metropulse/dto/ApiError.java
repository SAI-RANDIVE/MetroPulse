package com.metropulse.dto;

import java.time.Instant;
import java.util.List;

/** Uniform JSON error body for every /api endpoint. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldProblem> fieldErrors) {

    public record FieldProblem(String field, String message) {
    }
}
