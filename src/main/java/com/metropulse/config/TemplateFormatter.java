package com.metropulse.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Date helpers for Thymeleaf, used as ${@fmt.dateTime(x)}. All times are shown in Mumbai time. */
@Component("fmt")
public class TemplateFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ZoneId zone;
    private final Clock clock;

    public TemplateFormatter(MetroPulseProperties properties, Clock clock) {
        this.zone = properties.timeZone();
        this.clock = clock;
    }

    public String dateTime(Instant instant) {
        return instant == null ? "-" : DATE_TIME.format(instant.atZone(zone));
    }

    public String time(Instant instant) {
        return instant == null ? "-" : TIME.format(instant.atZone(zone));
    }

    /** "3 min ago", "in 12 min", "2 h ago" ... */
    public String relative(Instant instant) {
        if (instant == null) {
            return "-";
        }
        Duration d = Duration.between(clock.instant(), instant);
        boolean future = !d.isNegative();
        long minutes = Math.abs(d.toMinutes());
        String amount;
        if (minutes < 1) {
            return future ? "in under a minute" : "just now";
        } else if (minutes < 60) {
            amount = minutes + " min";
        } else if (minutes < 60 * 48) {
            amount = (minutes / 60) + " h";
        } else {
            amount = (minutes / (60 * 24)) + " days";
        }
        return future ? "in " + amount : amount + " ago";
    }

    public boolean isPast(Instant instant) {
        return instant != null && !instant.isAfter(clock.instant());
    }

    public String zoneLabel() {
        return "IST";
    }
}
