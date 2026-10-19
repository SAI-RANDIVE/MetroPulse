package com.metropulse.dto;

import com.metropulse.entity.MetroLine;

/** The minimum needed to draw a coloured line chip. */
public record LineBadge(long id, String code, String name, String color, String textColor) {

    public static LineBadge of(MetroLine line) {
        return new LineBadge(line.getId(), line.getLineCode(), line.getName(), line.getColor(), line.getTextColor());
    }
}
