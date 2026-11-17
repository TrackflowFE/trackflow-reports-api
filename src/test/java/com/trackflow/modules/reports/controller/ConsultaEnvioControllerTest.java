package com.trackflow.modules.reports.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.reports.api.ConsultaEnvioController;
import com.trackflow.modules.reports.api.ReportsExceptionHandler;
import com.trackflow.modules.reports.application.ConsultarEstadoEnvio;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.modules.reports.domain.TrackingNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ConsultaEnvioControllerTest {

    @Mock
    private ConsultarEstadoEnvio consultarEstadoEnvio;

    @InjectMocks
    private ConsultaEnvioController controller;

    private MockMvc mockMvc;

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ReportsExceptionHandler())
                .build();
    }

    private static ShipmentTrackingView entregado() {
        ShipmentTrackingView envio = new ShipmentTrackingView(
                "TRK-7",
                "DELIVERED",
                new ShipmentTrackingView.Origen("Luis Gómez", 1L, "Bogota"),
                new ShipmentTrackingView.Destino("Ana Torres", "Calle 7", 2L, "Medellin"),
                Instant.parse("2026-10-01T10:00:00Z"));
        envio.registrarMovimiento("DELIVERED", "Centro destino", 2L, "Medellin",
                Instant.parse("2026-10-02T10:00:00Z"));
        return envio;
    }

    private static UsernamePasswordAuthenticationToken operador() {
        return new UsernamePasswordAuthenticationToken("operador", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_OPERADOR")));
    }

    @Test
    void unAnonimoRecibeNombresEnmascaradosSinDireccion() throws Exception {
        when(consultarEstadoEnvio.ejecutar("TRK-7")).thenReturn(entregado());

        mockMvc.perform(get("/api/tracking/TRK-7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.trackingNumber").value("TRK-7"))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.senderName").value("L*** G***"))
                .andExpect(jsonPath("$.recipientName").value("A*** T***"))
                .andExpect(jsonPath("$.recipientAddress").doesNotExist())
                .andExpect(jsonPath("$.restricted").value(true))
                .andExpect(jsonPath("$.currentCity").value("Medellin"));
    }

    @Test
    void unOperadorRecibeLosDatosCompletos() throws Exception {
        when(consultarEstadoEnvio.ejecutar("TRK-7")).thenReturn(entregado());
        SecurityContextHolder.getContext().setAuthentication(operador());

        mockMvc.perform(get("/api/tracking/TRK-7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.senderName").value("Luis Gómez"))
                .andExpect(jsonPath("$.recipientName").value("Ana Torres"))
                .andExpect(jsonPath("$.recipientAddress").value("Calle 7"))
                .andExpect(jsonPath("$.restricted").value(false));
    }

    @Test
    void laDireccionSigueOcultaMientrasElEnvioNoEsteEntregado() throws Exception {
        ShipmentTrackingView envio = new ShipmentTrackingView(
                "TRK-8",
                "IN_TRANSIT",
                new ShipmentTrackingView.Origen("Remitente", 1L, "Bogota"),
                new ShipmentTrackingView.Destino("Destinatario", "Calle 8", 2L, "Medellin"),
                Instant.parse("2026-10-01T10:00:00Z"));
        when(consultarEstadoEnvio.ejecutar("TRK-8")).thenReturn(envio);
        SecurityContextHolder.getContext().setAuthentication(operador());

        mockMvc.perform(get("/api/tracking/TRK-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientAddress").doesNotExist())
                .andExpect(jsonPath("$.currentCityId").value(1));
    }

    @Test
    void devuelve404CuandoNoExisteElEnvio() throws Exception {
        when(consultarEstadoEnvio.ejecutar("NO-EXISTE"))
                .thenThrow(new TrackingNotFoundException("NO-EXISTE"));

        mockMvc.perform(get("/api/tracking/NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(
                        "No se encontró un envío con el número de seguimiento NO-EXISTE"));
    }
}
