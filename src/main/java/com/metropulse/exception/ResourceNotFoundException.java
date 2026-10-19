package com.metropulse.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends MetroPulseException {

    public ResourceNotFoundException(String what, Object id) {
        super(HttpStatus.NOT_FOUND, what + " " + id + " was not found");
    }
}
