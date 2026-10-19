package com.metropulse.controller.api;

import com.metropulse.dto.ApiError;
import com.metropulse.exception.MetroPulseException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/** Turns every exception from the JSON API into the same {@link ApiError} body with the right status. */
@RestControllerAdvice(basePackages = "com.metropulse.controller.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MetroPulseException.class)
    ResponseEntity<ApiError> business(MetroPulseException e, HttpServletRequest request) {
        return error(e.getStatus(), e.getMessage(), request, List.of());
    }

    /** @Valid failures on JSON bodies (MethodArgumentNotValidException) and query objects (BindException). */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiError> validation(BindException e, HttpServletRequest request) {
        List<ApiError.FieldProblem> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldProblem(f.getField(),
                        f.isBindingFailure() ? "Invalid value" : f.getDefaultMessage()))
                .toList();
        List<ApiError.FieldProblem> global = e.getBindingResult().getGlobalErrors().stream()
                .map(g -> new ApiError.FieldProblem(g.getObjectName(), g.getDefaultMessage())).toList();
        var all = new java.util.ArrayList<>(fields);
        all.addAll(global);
        return error(HttpStatus.BAD_REQUEST, "Some fields are invalid", request, all);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> methodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Some parameters are invalid", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Parameter '" + e.getName() + "' has an invalid value", request,
                List.of(new ApiError.FieldProblem(e.getName(), "Invalid value")));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> missingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Parameter '" + e.getParameterName() + "' is required", request,
                List.of(new ApiError.FieldProblem(e.getParameterName(), "Required")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON for this endpoint", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not supported here", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "No such endpoint", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "The change conflicts with existing data", request, List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> database(DataAccessException e, HttpServletRequest request) {
        log.error("Database error on {}", request.getRequestURI(), e);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "The database is not available right now", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", request, List.of());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request,
                                                  List<ApiError.FieldProblem> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields));
    }
}
