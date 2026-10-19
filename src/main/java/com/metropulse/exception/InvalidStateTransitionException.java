package com.metropulse.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends MetroPulseException {

    public InvalidStateTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
