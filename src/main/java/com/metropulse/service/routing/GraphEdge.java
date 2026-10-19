package com.metropulse.service.routing;

import com.metropulse.enums.EdgeType;

/** A directed, weighted connection between two graph nodes. */
public record GraphEdge(long id, long fromNodeId, long toNodeId, EdgeType type, int baseMinutes, String note) {

    public boolean isTransfer() {
        return type == EdgeType.TRANSFER;
    }
}
