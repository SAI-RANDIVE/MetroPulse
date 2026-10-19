package com.metropulse.exception;

import org.springframework.http.HttpStatus;

public class DuplicateAccountException extends MetroPulseException {

    public DuplicateAccountException() {
        super(HttpStatus.CONFLICT, "An account with this email already exists");
    }
}
