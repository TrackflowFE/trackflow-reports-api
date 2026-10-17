package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ProyectarTiemposDeTransito;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyectarTiemposDeTransitoTest {

    @Mock
    private ShipmentTransitViewRepository repository;

    @InjectMocks
    private ProyectarTiemposDeTransito proyeccion;

    @Test
    @DisplayName("crea la vista de tránsito solo cuando no existe")
    void creaLaVistaDeTransito() {
        // Arrange
        var event = new EnvioCreadoEvent("TF-100", "REGISTERED", "Ana", 1L, "Medellín",
                "Luis", "Calle 1", 2L, "Bogotá",
                Instant.parse("2026-10-01T12:00:00Z"), Instant.parse("2026-10-01T12:01:00Z"));
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.empty());

        // Act
        proyeccion.alCrearEnvio(event);

        // Assert
        verify(repository).save(any(ShipmentTransitView.class));
    }

    @Test
    @DisplayName("registra y persiste un hito de tránsito reconocido")
    void registraUnHitoReconocido() {
        // Arrange
        var view = new ShipmentTransitView("TF-100");
        var movedAt = Instant.parse("2026-10-02T12:00:00Z");
        var event = new EventoLogisticoRegistradoEvent(1L, "TF-100", "DISPATCHED",
                "IN_TRANSIT", "Centro Medellín", 10L, 1L, "Medellín",
                movedAt, movedAt.plusSeconds(60), movedAt.plusSeconds(120));
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.of(view));

        // Act
        proyeccion.alRegistrarEvento(event);

        // Assert
        assertThat(view.getDispatchedAt()).isEqualTo(movedAt);
        verify(repository).save(view);
    }

    @Test
    @DisplayName("ignora eventos que no representan un hito del recorrido")
    void ignoraUnEventoNoReconocido() {
        // Arrange
        var event = new EventoLogisticoRegistradoEvent(1L, "TF-100", "CANCELLED",
                "CANCELLED", "Centro Medellín", 10L, 1L, "Medellín",
                Instant.parse("2026-10-02T12:00:00Z"), Instant.parse("2026-10-02T12:01:00Z"),
                Instant.parse("2026-10-02T12:02:00Z"));

        // Act
        proyeccion.alRegistrarEvento(event);

        // Assert
        verifyNoInteractions(repository);
    }
}
