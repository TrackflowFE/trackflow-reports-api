package com.trackflow.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trackflow.modules.reports.application.ProyectarVolumenDeEnvios;
import com.trackflow.modules.reports.application.ShipmentVolumeViewRepository;
import com.trackflow.modules.reports.domain.ShipmentVolumeView;
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
class ProyectarVolumenDeEnviosTest {

    @Mock
    private ShipmentVolumeViewRepository repository;

    @InjectMocks
    private ProyectarVolumenDeEnvios proyeccion;

    @Test
    @DisplayName("proyecta un envío nuevo y conserva su ciudad de origen")
    void proyectaUnEnvioNuevo() {
        // Arrange
        var registrado = Instant.parse("2026-10-01T12:00:00Z");
        var event = new EnvioCreadoEvent("TF-100", "REGISTERED", "Ana", 1L, "Medellín",
                "Luis", "Calle 1", 2L, "Bogotá", registrado,
                Instant.parse("2026-10-01T12:01:00Z"));
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.empty());

        // Act
        proyeccion.alCrearEnvio(event);

        // Assert
        var captor = ArgumentCaptor.forClass(ShipmentVolumeView.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTrackingNumber()).isEqualTo("TF-100");
        assertThat(captor.getValue().getOriginCity()).isEqualTo("Medellín");
        assertThat(captor.getValue().getRegisteredAt()).isEqualTo(registrado);
    }

    @Test
    @DisplayName("registra el ingreso a un centro y persiste la vista")
    void registraElPrimerIngreso() {
        // Arrange
        var view = new ShipmentVolumeView("TF-100", Instant.parse("2026-10-01T12:00:00Z"),
                1L, "Medellín");
        var event = new EventoLogisticoRegistradoEvent(1L, "TF-100", "RECEIVED_AT_CENTER",
                "AT_DISTRIBUTION_CENTER", "Centro Medellín", 10L, 1L, "Medellín",
                Instant.parse("2026-10-02T12:00:00Z"), Instant.parse("2026-10-02T12:01:00Z"),
                Instant.parse("2026-10-02T12:02:00Z"));
        when(repository.findByTrackingNumber("TF-100")).thenReturn(Optional.of(view));

        // Act
        proyeccion.alRegistrarEvento(event);

        // Assert
        assertThat(view.getEntryCenterId()).isEqualTo(10L);
        assertThat(view.getEntryPoint()).isEqualTo("Centro Medellín");
        assertThat(view.getEntryCity()).isEqualTo("Medellín");
        verify(repository).save(view);
    }

    @Test
    @DisplayName("ignora los eventos que no cambian el reporte de volumen")
    void ignoraUnEventoNoRelacionado() {
        // Arrange
        var event = new EventoLogisticoRegistradoEvent(1L, "TF-100", "DISPATCHED",
                "IN_TRANSIT", "Centro Medellín", 10L, 1L, "Medellín",
                Instant.parse("2026-10-02T12:00:00Z"), Instant.parse("2026-10-02T12:01:00Z"),
                Instant.parse("2026-10-02T12:02:00Z"));

        // Act
        proyeccion.alRegistrarEvento(event);

        // Assert
        verifyNoInteractions(repository);
    }
}
