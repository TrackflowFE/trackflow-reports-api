package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ProyectarSeguimientoEnvio;
import com.trackflow.modules.reports.application.ShipmentTrackingViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectarSeguimientoEnvioTest {

    @Mock
    private ShipmentTrackingViewRepository repository;

    @InjectMocks
    private ProyectarSeguimientoEnvio proyeccion;

    @Test
    @DisplayName("crea la vista de seguimiento si el envío aún no está proyectado")
    void creaLaVistaDeSeguimiento() {
        // Arrange
        var event = new EnvioCreadoEvent("TF-100", "REGISTERED", "Ana", 1L, "Medellín",
                "Luis", "Calle 1", 2L, "Bogotá",
                Instant.parse("2026-10-01T12:00:00Z"), Instant.parse("2026-10-01T12:01:00Z"));
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.empty());
        when(repository.save(any(ShipmentTrackingView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        proyeccion.alCrearEnvio(event);

        // Assert
        var captor = ArgumentCaptor.forClass(ShipmentTrackingView.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTrackingNumber()).isEqualTo("TF-100");
        assertThat(captor.getValue().getStatus()).isEqualTo("REGISTERED");
        assertThat(captor.getValue().getOriginCity()).isEqualTo("Medellín");
    }

    @Test
    @DisplayName("no reemplaza una vista ya existente al reprocesar el alta")
    void noDuplicaUnaVistaExistente() {
        // Arrange
        var event = new EnvioCreadoEvent("TF-100", "REGISTERED", "Ana", 1L, "Medellín",
                "Luis", "Calle 1", 2L, "Bogotá",
                Instant.parse("2026-10-01T12:00:00Z"), Instant.parse("2026-10-01T12:01:00Z"));
        when(repository.findByTrackingNumber("TF-100"))
                .thenReturn(Optional.of(new ShipmentTrackingView("TF-100", "IN_TRANSIT",
                        new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                        new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                        Instant.parse("2026-10-01T12:00:00Z"))));

        // Act
        proyeccion.alCrearEnvio(event);

        // Assert
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("guarda la vista cuando aplica un movimiento logístico")
    void guardaUnMovimientoAplicado() {
        // Arrange
        var view = new ShipmentTrackingView("TF-100", "REGISTERED",
                new ShipmentTrackingView.Origen("Ana", 1L, "Medellín"),
                new ShipmentTrackingView.Destino("Luis", "Calle 1", 2L, "Bogotá"),
                Instant.parse("2026-10-01T12:00:00Z"));
        var event = new EventoLogisticoRegistradoEvent(1L, "TF-100", "DISPATCHED", "IN_TRANSIT",
                "Centro Medellín", 10L, 1L, "Medellín",
                Instant.parse("2026-10-02T12:00:00Z"), Instant.parse("2026-10-02T12:01:00Z"),
                Instant.parse("2026-10-02T12:02:00Z"), 2L, "Bogotá", 9);
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.of(view));

        // Act
        proyeccion.alRegistrarEvento(event);

        // Assert
        assertThat(view.getStatus()).isEqualTo("IN_TRANSIT");
        assertThat(view.getNextStopCity()).isEqualTo("Bogotá");
        verify(repository).save(view);
    }
}
