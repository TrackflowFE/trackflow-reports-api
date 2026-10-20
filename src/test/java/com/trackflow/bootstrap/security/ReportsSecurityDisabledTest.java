package com.trackflow.bootstrap.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "trackflow.security.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportsSecurityDisabledTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void emptyProjections() {
        jdbc.update("delete from reports_shipment_tracking");
        jdbc.update("delete from reports_shipment_transit");
        jdbc.update("delete from reports_shipment_volume");
    }

    @ParameterizedTest
    @ValueSource(strings = {"delayed-shipments", "transit-times", "shipment-volume"})
    void reportesPublicosSinValidarBearerPeroEscriturasDenegadas(String report) throws Exception {
        String route = "/api/reports/" + report;
        mvc.perform(get(route)).andExpect(status().isOk());
        mvc.perform(get(route).header("Authorization", "Bearer invalid")).andExpect(status().isOk());
        mvc.perform(post(route)).andExpect(status().isForbidden());
    }

    @Test
    void publicosPreflightYDenegacionFinalSinSeguridad() throws Exception {
        mvc.perform(get("/").header("Authorization", "Bearer invalid")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/actuator/health").header("Authorization", "Bearer invalid")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs").header("Authorization", "Bearer invalid")).andExpect(status().isOk());
        mvc.perform(get("/api/tracking/UNKNOWN").header("Authorization", "Bearer invalid"))
                .andExpect(status().isNotFound());
        mvc.perform(options("/unmatched").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
        mvc.perform(get("/unmatched")).andExpect(status().isForbidden());
        mvc.perform(post("/api/tracking/UNKNOWN")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login")).andExpect(status().isForbidden());
    }
}
