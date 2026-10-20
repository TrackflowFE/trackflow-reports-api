package com.trackflow.modules.reports.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConsultarTiemposDeTransitoTest {
    private final ShipmentTransitViewRepository repository = mock(ShipmentTransitViewRepository.class);
    private final ConsultarTiemposDeTransito query = new ConsultarTiemposDeTransito(repository,
            Clock.fixed(Instant.parse("2026-10-10T17:00:00Z"), ZoneOffset.UTC));
    private final LocalDate day = LocalDate.of(2026, 10, 2);

    @ParameterizedTest
    @ValueSource(strings = {"absent", "null-average", "present"})
    void medicionesAusentesYPromediosNulosConservanOrdenYConteos(String totalState) {
        var measurements = new HashMap<String, Medicion>();
        TramoDeTransito[] segments = TramoDeTransito.values();
        measurements.put(segments[1].codigo(), new Medicion(null, 3));
        measurements.put(segments[2].codigo(), new Medicion(Duration.ofSeconds(91), 2));
        measurements.put(segments[3].codigo(), null);
        if (!"absent".equals(totalState)) {
            measurements.put(ShipmentTransitViewRepository.TOTAL,
                    new Medicion("present".equals(totalState) ? Duration.ofHours(4) : null, 5));
        }
        when(repository.contarEntregadosEntre(any(), any())).thenReturn(5L);
        when(repository.medirEntregadosEntre(any(), any())).thenReturn(measurements);

        var result = query.ejecutar(day, day);

        assertEquals(5, result.enviosEntregados());
        assertEquals("present".equals(totalState) ? Duration.ofHours(4) : null, result.promedioTotal());
        assertEquals(List.of(segments), result.tramos().stream().map(TiemposDeTransito.PromedioDeTramo::tramo).toList());
        assertNull(result.tramos().get(0).promedio());
        assertEquals(0, result.tramos().get(0).envios());
        assertNull(result.tramos().get(1).promedio());
        assertEquals(3, result.tramos().get(1).envios());
        assertEquals(Duration.ofSeconds(91), result.tramos().get(2).promedio());
        assertEquals(2, result.tramos().get(2).envios());
        assertNull(result.tramos().get(3).promedio());
        assertEquals(0, result.tramos().get(3).envios());
    }

    @Test
    void sinEntregasNoConsultaMediciones() {
        var result = query.ejecutar(day, day);
        assertFalse(result.hayDatos());
        assertNull(result.promedioTotal());
        assertTrue(result.tramos().isEmpty());
        verify(repository, never()).medirEntregadosEntre(any(), any());
    }
}
