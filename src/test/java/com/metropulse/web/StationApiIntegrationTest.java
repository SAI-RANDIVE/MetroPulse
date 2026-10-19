package com.metropulse.web;

import com.metropulse.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StationApiIntegrationTest extends IntegrationTest {

    @Test
    void partialSearchIgnoresCaseSpacesAndPunctuation() throws Exception {
        mvc.perform(get("/api/stations").param("query", "dn nagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("D N Nagar"));
        mvc.perform(get("/api/stations").param("query", "ANDHERI W"))
                .andExpect(jsonPath("$[0].name").value("Andheri (West)"));
        mvc.perform(get("/api/stations").param("query", "ghat"))
                .andExpect(jsonPath("$[*].name", hasItem("Ghatkopar")));
        mvc.perform(get("/api/stations").param("query", "zzz"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsAllStationsWithLines() throws Exception {
        mvc.perform(get("/api/stations"))
                .andExpect(jsonPath("$", hasSize(42)))
                .andExpect(jsonPath("$[?(@.name == 'Dahisar (East)')].lines[*].code", containsInAnyOrder("2A", "7")));
    }

    @Test
    void stationDetailAndStatus() throws Exception {
        long id = stationId("WEH");
        mvc.perform(get("/api/stations/{id}", id))
                .andExpect(jsonPath("$.name").value("Western Express Highway"));
        mvc.perform(get("/api/stations/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crowd[0].state").value("NO_DATA"))
                .andExpect(jsonPath("$.crowd[0].level").value(nullValue()))
                .andExpect(jsonPath("$.accessibility").value("UNKNOWN"));
    }

    @Test
    void unknownAndInvalidIds() throws Exception {
        mvc.perform(get("/api/stations/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
        mvc.perform(get("/api/stations/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    void linesApiReturnsOrderedStops() throws Exception {
        mvc.perform(get("/api/lines"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].code").value("2A"))
                .andExpect(jsonPath("$[1].stops[0].stationName").value("Dahisar (East)"))
                .andExpect(jsonPath("$[1].stops[16].stationName").value("Andheri (West)"));
    }

    @Test
    void stationPagesRender() throws Exception {
        mvc.perform(get("/stations").param("q", "malad"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lower Malad")));
        mvc.perform(get("/stations/{id}", stationId("DAHISAR_EAST")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Interchanges")))
                .andExpect(content().string(containsString("No recent reports")));
        mvc.perform(get("/lines/7")).andExpect(status().isOk()).andExpect(content().string(containsString("Ovaripada")));
        mvc.perform(get("/lines/99")).andExpect(status().isNotFound());
    }
}
