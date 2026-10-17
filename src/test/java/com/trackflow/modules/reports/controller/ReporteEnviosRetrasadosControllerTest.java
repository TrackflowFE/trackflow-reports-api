package com.trackflow.modules.reports.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.reports.api.ReporteEnviosRetrasadosController;
import com.trackflow.modules.reports.application.ConsultarEnviosRetrasados;
import com.trackflow.modules.reports.application.EnviosRetrasados;
import com.trackflow.modules.reports.domain.Severidad;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReporteEnviosRetrasadosControllerTest {

    @Mock
    private ConsultarEnviosRetrasados consultar;

    @InjectMocks
    private ReporteEnviosRetrasadosController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void devuelve200ConMensajeCuandoNoHayEnviosRetrasados() throws Exception {
        // Arrange
        when(consultar.ejecutar()).thenReturn(new EnviosRetrasados(Instant.parse("2026-10-16T12:00:00Z"), List.of()));

        // Act / Assert
        mockMvc.perform(get("/api/reports/delayed-shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDelayed").value(0))
                .andExpect(jsonPath("$.message").value("No hay envíos retrasados en este momento"))
                .andExpect(jsonPath("$.shipments").isEmpty());
    }

    @Test
    void devuelveElDetalleDeLosEnviosRetrasados() throws Exception {
        // Arrange
        EnviosRetrasados.Retraso retraso = new EnviosRetrasados.Retraso(
                "TRK-9", "IN_TRANSIT", "Centro norte", "Bogota", "Cali",
                Duration.ofHours(12), Duration.ofHours(8), Severidad.MODERADA,
                "Cali", 6);
        when(consultar.ejecutar()).thenReturn(
                new EnviosRetrasados(Instant.parse("2026-10-16T12:00:00Z"), List.of(retraso)));

        // Act / Assert
        mockMvc.perform(get("/api/reports/delayed-shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDelayed").value(1))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.shipments[0].trackingNumber").value("TRK-9"))
                .andExpect(jsonPath("$.shipments[0].hoursOver").value(4))
                .andExpect(jsonPath("$.shipments[0].severity").value("MODERATE"))
                .andExpect(jsonPath("$.shipments[0].nextStop").value("Cali"));
    }
}
