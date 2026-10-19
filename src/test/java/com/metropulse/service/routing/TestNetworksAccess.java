package com.metropulse.service.routing;

import com.metropulse.config.MetroPulseProperties;

/** Gives tests in other packages the shared test configuration. */
public final class TestNetworksAccess {

    private TestNetworksAccess() {
    }

    public static MetroPulseProperties properties() {
        return TestNetworks.properties();
    }
}
