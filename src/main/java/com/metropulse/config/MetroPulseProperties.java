package com.metropulse.config;

import com.metropulse.enums.CrowdLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

/**
 * All tunable MetroPulse settings, bound from the "metropulse" section of application.yml.
 * Keeping the numbers here avoids magic constants scattered across the code.
 */
@ConfigurationProperties(prefix = "metropulse")
public record MetroPulseProperties(
        @DefaultValue("development") String environment,
        @DefaultValue("Asia/Kolkata") ZoneId timeZone,
        @DefaultValue Security security,
        @DefaultValue Demo demo,
        @DefaultValue Crowd crowd,
        @DefaultValue Routing routing) {

    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }

    public record Security(String adminEmail, String adminPassword,
                           @DefaultValue("MetroPulse Admin") String adminDisplayName) {
    }

    public record Demo(@DefaultValue("true") boolean enabled) {
    }

    public record Crowd(
            @DefaultValue("15m") Duration reportTtl,
            @DefaultValue("6h") Duration staleWindow,
            @DefaultValue("5m") Duration minIntervalSameStation,
            @DefaultValue("6") int maxReportsPerHour) {
    }

    public record Routing(Map<CrowdLevel, Integer> crowdPenalty,
                          @DefaultValue("20") int transferPenaltyMinutes) {

        public Routing {
            EnumMap<CrowdLevel, Integer> penalties = new EnumMap<>(CrowdLevel.class);
            for (CrowdLevel level : CrowdLevel.values()) {
                int value = crowdPenalty == null ? 0 : crowdPenalty.getOrDefault(level, 0);
                if (value < 0) {
                    throw new IllegalArgumentException("Crowd penalties must not be negative (Dijkstra needs non-negative costs)");
                }
                penalties.put(level, value);
            }
            if (transferPenaltyMinutes < 0) {
                throw new IllegalArgumentException("transfer-penalty-minutes must not be negative");
            }
            crowdPenalty = Map.copyOf(penalties);
        }

        public int penaltyFor(CrowdLevel level) {
            return crowdPenalty.getOrDefault(level, 0);
        }
    }
}
