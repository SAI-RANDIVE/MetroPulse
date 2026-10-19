package com.metropulse.exception;

import org.springframework.http.HttpStatus;

/** Base class for expected business errors. Each subclass maps to one HTTP status. */
public abstract class MetroPulseException extends RuntimeException {

    private final HttpStatus status;

    protected MetroPulseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
