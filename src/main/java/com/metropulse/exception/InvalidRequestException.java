package com.metropulse.exception;

import org.springframework.http.HttpStatus;

/** The request is well-formed but breaks a business rule (e.g. inconsistent line and station). */
public class InvalidRequestException extends MetroPulseException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
