package com.metropulse.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level check that a journey's origin and destination are not the same station. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DifferentStationsValidator.class)
public @interface DifferentStations {

    String message() default "Origin and destination must be different stations";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
