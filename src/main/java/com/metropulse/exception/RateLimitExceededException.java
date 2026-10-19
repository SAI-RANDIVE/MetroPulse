package com.metropulse.exception;

import org.springframework.http.HttpStatus;

/** Anti-spam rule for crowd reports. */
public class RateLimitExceededException extends MetroPulseException {

    public RateLimitExceededException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
