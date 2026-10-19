package com.metropulse.controller.web;

import com.metropulse.exception.MetroPulseException;
import com.metropulse.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Friendly error pages for browser users. Messages are our own, never stack traces. */
@ControllerAdvice(basePackages = "com.metropulse.controller.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler({ResourceNotFoundException.class, MethodArgumentTypeMismatchException.class})
    String notFound(Exception e, Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("message", e instanceof ResourceNotFoundException ? e.getMessage() : "That page does not exist.");
        return "error/404";
    }

    @ExceptionHandler(MetroPulseException.class)
    String business(MetroPulseException e, Model model, HttpServletResponse response) {
        response.setStatus(e.getStatus().value());
        model.addAttribute("status", e.getStatus().value());
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    @ExceptionHandler(DataAccessException.class)
    String database(DataAccessException e, Model model, HttpServletResponse response) {
        log.error("Database error while rendering a page", e);
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("status", 503);
        model.addAttribute("message", "The database is not reachable right now. Please try again shortly.");
        return "error";
    }
}
