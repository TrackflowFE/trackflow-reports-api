package com.trackflow.modules.reports.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trackflow.modules.reports.domain.ShipmentVolumeView;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProyectarVolumenDeEnviosTest {
    private static final Instant AT = Instant.parse("2026-10-01T12:00:00Z");
    private final ShipmentVolumeViewRepository repository = mock(ShipmentVolumeViewRepository.class);
    private final ProyectarVolumenDeEnvios projection = new ProyectarVolumenDeEnvios(repository);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"UNKNOWN", "DISPATCHED", "delivered", ""})
    void tiposNoAplicablesNoConsultanRepositorio(String type) {
        projection.alRegistrarEvento(event(type, AT, false));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEIVED_AT_CENTER", "DELIVERED"})
    void sinVistaNoGuarda(String type) {
        when(repository.findByTrackingNumber("TF-TEST")).thenReturn(Optional.empty());
        projection.alRegistrarEvento(event(type, AT, false));
        verify(repository).findByTrackingNumber("TF-TEST");
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"RECEIVED_AT_CENTER,false", "RECEIVED_AT_CENTER,true", "DELIVERED,false", "DELIVERED,true"})
    void guardaSoloPrimerEventoYAnterioresIncluidosHistoricos(String type, boolean historical) {
        var view = new ShipmentVolumeView("TF-TEST", AT.minusSeconds(60), 1L, "Origin");
        when(repository.findByTrackingNumber("TF-TEST")).thenReturn(Optional.of(view));
        projection.alRegistrarEvento(event(type, AT, historical));
        verify(repository).save(view);
        assertFields(view, type, AT, historical);

        clearInvocations(repository);
        projection.alRegistrarEvento(event(type, AT, historical));
        projection.alRegistrarEvento(event(type, AT.plusSeconds(1), !historical));
        verify(repository, never()).save(any());
        assertFields(view, type, AT, historical);

        projection.alRegistrarEvento(event(type, AT.minusSeconds(1), historical));
        verify(repository).save(view);
        assertFields(view, type, AT.minusSeconds(1), historical);
        assertEquals(AT.minusSeconds(60), view.getRegisteredAt());
        assertEquals(1L, view.getOriginCityId());
        assertEquals("Origin", view.getOriginCity());
    }

    private void assertFields(ShipmentVolumeView view, String type, Instant at, boolean historical) {
        if ("RECEIVED_AT_CENTER".equals(type)) {
            assertEquals(at, view.getReceivedAt());
            assertNull(view.getDeliveredAt());
            assertEquals(historical ? null : 10L, view.getEntryCenterId());
            assertEquals("Center", view.getEntryPoint());
            assertEquals(historical ? null : 2L, view.getEntryCityId());
            assertEquals(historical ? null : "City", view.getEntryCity());
        } else {
            assertEquals(at, view.getDeliveredAt());
            assertNull(view.getReceivedAt());
            assertNull(view.getEntryCenterId());
            assertNull(view.getEntryPoint());
            assertNull(view.getEntryCityId());
            assertNull(view.getEntryCity());
        }
    }

    private EventoLogisticoRegistradoEvent event(String type, Instant at, boolean historical) {
        return new EventoLogisticoRegistradoEvent(1L, "TF-TEST", type, null, "Center",
                historical ? null : 10L, historical ? null : 2L, historical ? null : "City",
                at, AT.plusSeconds(100), AT.plusSeconds(200), null, null, null);
    }
}
