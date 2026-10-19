package com.metropulse.service.routing;

/**
 * A node of the routing graph: one station on one line (a {@code line_station} row).
 *
 * @param stopping false when trains do not stop here (station closed or node not operational);
 *                 such a node can be passed through but not used to board, alight or change.
 */
public record GraphNode(long id,
                        long stationId,
                        String stationName,
                        long lineId,
                        String lineCode,
                        String lineName,
                        String lineColor,
                        String lineTextColor,
                        int sequenceNo,
                        boolean stopping) {
}
