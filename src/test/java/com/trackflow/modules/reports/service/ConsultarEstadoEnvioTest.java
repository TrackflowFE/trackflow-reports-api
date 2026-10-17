package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ConsultarEstadoEnvio;
import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.modules.reports.domain.TrackingNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoEnvioTest {

    @Mock
    private ShipmentTrackingViewRepository repository;

    @InjectMocks
    private ConsultarEstadoEnvio consulta;

    @Test
    @DisplayName("devuelve el estado del envío encontrado")
    void devuelveElEnvioEncontrado() {
        // Arrange
        var trackingNumber = "TF-100";
        var view = new ShipmentTrackingView(trackingNumber, "IN_TRANSIT",
                new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                Instant.parse("2026-10-01T12:00:00Z"));
        when(repository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(view));

        // Act
        ShipmentTrackingView resultado = consulta.ejecutar(trackingNumber);

        // Assert
        assertThat(resultado).isSameAs(view);
        verify(repository).findByTrackingNumber(trackingNumber);
    }

    @Test
    @DisplayName("lanza una excepción cuando el envío no existe")
    void fallaCuandoElEnvioNoExiste() {
        // Arrange
        var trackingNumber = "TF-404";
        when(repository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

        // Act / Assert
        assertThatThrownBy(() -> consulta.ejecutar(trackingNumber))
                .isInstanceOf(TrackingNotFoundException.class)
                .hasMessageContaining(trackingNumber);
        verify(repository).findByTrackingNumber(trackingNumber);
    }
}
