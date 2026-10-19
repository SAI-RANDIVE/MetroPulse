package com.metropulse.exception;

import org.springframework.http.HttpStatus;

/** Both stations exist but no usable connection joins them right now. */
public class RouteNotFoundException extends MetroPulseException {

    public RouteNotFoundException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }
}
