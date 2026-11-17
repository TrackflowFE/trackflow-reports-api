package com.trackflow.modules.reports.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.reports.api.LimitesDePeriodoController;
import com.trackflow.modules.reports.application.LimitesDePeriodo;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LimitesDePeriodoControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new LimitesDePeriodoController(new LimitesDePeriodo(LocalDate.of(2026, 1, 1), 366))).build();
    }

    @Test
    void devuelveLaFechaMinimaYElMaximoDeDias() throws Exception {
        mockMvc.perform(get("/api/reports/period-limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minDate").value("2026-01-01"))
                .andExpect(jsonPath("$.maxDays").value(366));
    }
}
