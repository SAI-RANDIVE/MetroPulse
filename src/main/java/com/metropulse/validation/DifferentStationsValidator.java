package com.metropulse.validation;

import com.metropulse.dto.form.RouteSearchForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DifferentStationsValidator implements ConstraintValidator<DifferentStations, RouteSearchForm> {

    @Override
    public boolean isValid(RouteSearchForm form, ConstraintValidatorContext context) {
        if (form == null || form.getFrom() == null || form.getTo() == null) {
            return true; // @NotNull on the fields reports missing values
        }
        if (!form.getFrom().equals(form.getTo())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("to").addConstraintViolation();
        return false;
    }
}
