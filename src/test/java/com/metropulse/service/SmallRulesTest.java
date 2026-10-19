package com.metropulse.service;

import com.metropulse.entity.Station;
import com.metropulse.entity.StationFacility;
import com.metropulse.enums.*;
import com.metropulse.service.routing.CostBreakdown;
import com.metropulse.service.routing.RouteExplainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Alert workflow, accessibility rule and route explanations. */
class SmallRulesTest {

    @Test
    void alertWorkflowTransitions() {
        assertThat(AlertStatus.DRAFT.canTransitionTo(AlertStatus.ACTIVE)).isTrue();
        assertThat(AlertStatus.DRAFT.canTransitionTo(AlertStatus.CANCELLED)).isTrue();
        assertThat(AlertStatus.DRAFT.canTransitionTo(AlertStatus.RESOLVED)).isFalse();
        assertThat(AlertStatus.ACTIVE.canTransitionTo(AlertStatus.RESOLVED)).isTrue();
        assertThat(AlertStatus.ACTIVE.canTransitionTo(AlertStatus.DRAFT)).isFalse();
        assertThat(AlertStatus.RESOLVED.allowedNext()).isEmpty();
        assertThat(AlertStatus.CANCELLED.allowedNext()).isEmpty();
        assertThat(AlertStatus.RESOLVED.isEditable()).isFalse();
    }

    private final Station station = new Station("X", "X", null, StationStatus.OPEN);

    private StationFacility facility(FacilityType type, FacilityStatus status) {
        return new StationFacility(station, type, status);
    }

    @Test
    void accessibilityNeedsAWorkingStepFreeFacility() {
        assertThat(FacilityService.evaluate(List.of())).isEqualTo(AccessibilityState.UNKNOWN);
        assertThat(FacilityService.evaluate(List.of(facility(FacilityType.ESCALATOR, FacilityStatus.AVAILABLE))))
                .isEqualTo(AccessibilityState.UNKNOWN); // an escalator is not step-free
        assertThat(FacilityService.evaluate(List.of(facility(FacilityType.LIFT, FacilityStatus.UNKNOWN))))
                .isEqualTo(AccessibilityState.UNKNOWN);
        assertThat(FacilityService.evaluate(List.of(facility(FacilityType.LIFT, FacilityStatus.OUT_OF_SERVICE))))
                .isEqualTo(AccessibilityState.NOT_STEP_FREE);
        assertThat(FacilityService.evaluate(List.of(facility(FacilityType.LIFT, FacilityStatus.OUT_OF_SERVICE),
                facility(FacilityType.WHEELCHAIR_ACCESS, FacilityStatus.AVAILABLE))))
                .isEqualTo(AccessibilityState.STEP_FREE_RECORDED);
    }

    private static CostBreakdown cost(int ride, int crowd, int withData) {
        return new CostBreakdown(ride, 0, 0, crowd, 0, 0, withData, ride);
    }

    @Test
    void explanationsNeverClaimLessCrowdingWithoutData() {
        String noData = RouteExplainer.explain(RoutePreference.LESS_CROWDED, cost(20, 0, 0), cost(20, 0, 0),
                1, 1, true, false, 20, null);
        assertThat(noData).contains("crowding is unknown").doesNotContain("fewer reported crowding");

        String better = RouteExplainer.explain(RoutePreference.LESS_CROWDED, cost(30, 0, 3), cost(20, 18, 3),
                1, 2, false, true, 20, null);
        assertThat(better).contains("fewer reported crowding issues").contains("10 extra minutes");

        String fastest = RouteExplainer.explain(RoutePreference.FASTEST, cost(20, 0, 0), cost(20, 0, 0),
                0, 0, true, false, 20, "Signal failure");
        assertThat(fastest).contains("Fastest based on configured estimated travel times").contains("Signal failure");
    }

    @Test
    void stationSearchNormalisation() {
        assertThat(StationService.normalize("D. N. Nagar")).isEqualTo("dnnagar");
        assertThat(StationService.normalize("Andheri (West)")).isEqualTo("andheriwest");
        assertThat(StationService.normalize(null)).isEmpty();
    }
}
