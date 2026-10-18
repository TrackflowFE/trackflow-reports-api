package com.trackflow.modules.reports.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.reports.api.ReporteTiemposDeTransitoController;
import com.trackflow.modules.reports.api.ReporteVolumenExceptionHandler;
import com.trackflow.modules.reports.application.ConsultarTiemposDeTransito;
import com.trackflow.modules.reports.application.TiemposDeTransito;
import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import java.time.Instant;
import java.time.LocalDate;
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
class ReporteTiemposDeTransitoControllerTest {

    @Mock
    private ConsultarTiemposDeTransito consultarTiempos;

    @InjectMocks
    private ReporteTiemposDeTransitoController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ReporteVolumenExceptionHandler())
                .build();
    }

    @Test
    void devuelve200ConElReporteYConvierteLosParametrosDeFecha() throws Exception {
        // Arrange
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 15);
        when(consultarTiempos.ejecutar(desde, hasta)).thenReturn(
                new TiemposDeTransito(desde, hasta, Instant.parse("2026-10-16T00:00:00Z"),
                        false, 0, null, List.of()));

        // Act
        var resultado = mockMvc.perform(get("/api/reports/transit-times")
                .param("from", "2026-10-01")
                .param("to", "2026-10-15"));

        // Assert
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.to").value("2026-10-15"))
                .andExpect(jsonPath("$.deliveredShipments").value(0))
                .andExpect(jsonPath("$.averageTotal").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        "No hay datos para el periodo consultado: no se entregó ningún envío"));
        verify(consultarTiempos).ejecutar(desde, hasta);
    }

    @Test
    void devuelve400CuandoElPeriodoEsInvalido() throws Exception {
        // Arrange
        LocalDate desde = LocalDate.of(2026, 10, 15);
        LocalDate hasta = LocalDate.of(2026, 10, 1);
        when(consultarTiempos.ejecutar(desde, hasta))
                .thenThrow(new PeriodoInvalidoException("La fecha inicial es posterior a la fecha final"));

        // Act / Assert
        mockMvc.perform(get("/api/reports/transit-times")
                        .param("from", "2026-10-15")
                        .param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Periodo inválido"))
                .andExpect(jsonPath("$.detail").value("La fecha inicial es posterior a la fecha final"));
    }

    @Test
    void devuelve400CuandoLaFechaNoTieneElFormatoEsperado() throws Exception {
        // Arrange

        // Act / Assert
        mockMvc.perform(get("/api/reports/transit-times").param("from", "15-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Fecha inválida"))
                .andExpect(jsonPath("$.detail").value(
                        "'15-10-2026' no es una fecha válida para 'from'; use el formato AAAA-MM-DD"));
    }
}
