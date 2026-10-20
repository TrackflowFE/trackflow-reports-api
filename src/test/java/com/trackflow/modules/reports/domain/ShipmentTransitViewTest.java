package com.trackflow.modules.reports.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ShipmentTransitViewTest {
    private static final Instant AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final Map<HitoDelRecorrido, Function<ShipmentTransitView, Instant>> GETTERS = Map.of(
            HitoDelRecorrido.RECEIVED_AT_CENTER, ShipmentTransitView::getReceivedAt,
            HitoDelRecorrido.DISPATCHED, ShipmentTransitView::getDispatchedAt,
            HitoDelRecorrido.ARRIVED_AT_DESTINATION_CENTER, ShipmentTransitView::getArrivedAt,
            HitoDelRecorrido.OUT_FOR_DELIVERY, ShipmentTransitView::getOutForDeliveryAt,
            HitoDelRecorrido.DELIVERED, ShipmentTransitView::getDeliveredAt);

    @ParameterizedTest
    @EnumSource(HitoDelRecorrido.class)
    void conservaElPrimeroOAnteriorYEscribeSoloSuCampo(HitoDelRecorrido hito) {
        var view = new ShipmentTransitView("TF-TEST");
        assertEquals(Set.of(HitoDelRecorrido.values()), GETTERS.keySet());
        assertEquals("TF-TEST", view.getTrackingNumber());
        assertNull(view.get(hito));
        assertTrue(view.registrar(hito, AT));
        assertFalse(view.registrar(hito, AT));
        assertFalse(view.registrar(hito, AT.plusSeconds(1)));
        assertEquals(AT, view.get(hito));
        assertTrue(view.registrar(hito, AT.minusSeconds(1)));
        assertFalse(view.registrar(hito, AT));
        for (HitoDelRecorrido other : HitoDelRecorrido.values()) {
            Instant expected = other == hito ? AT.minusSeconds(1) : null;
            assertEquals(expected, view.get(other));
            assertEquals(expected, GETTERS.get(other).apply(view));
        }
    }

    @ParameterizedTest
    @EnumSource(HitoDelRecorrido.class)
    void fechaNulaSoloFallaCuandoYaHayFecha(HitoDelRecorrido hito) {
        var view = new ShipmentTransitView("TF-TEST");
        assertTrue(view.registrar(hito, null));
        assertNull(view.get(hito));
        assertTrue(view.registrar(hito, AT));
        assertThrows(NullPointerException.class, () -> view.registrar(hito, null));
        assertEquals(AT, view.get(hito));
    }

    @Test
    void hitoNuloFallaEnLecturaYRegistro() {
        var view = new ShipmentTransitView("TF-TEST");
        assertThrows(NullPointerException.class, () -> view.get(null));
        assertThrows(NullPointerException.class, () -> view.registrar(null, AT));
        assertThrows(NullPointerException.class, () -> view.registrar(null, null));
    }

    @ParameterizedTest
    @EnumSource(HitoDelRecorrido.class)
    void nombresExactosReconocidosSinNormalizacion(HitoDelRecorrido hito) {
        assertEquals(hito, HitoDelRecorrido.deTipoDeEvento(hito.name()).orElseThrow());
        assertTrue(HitoDelRecorrido.deTipoDeEvento(hito.name().toLowerCase(java.util.Locale.ROOT)).isEmpty());
        assertTrue(HitoDelRecorrido.deTipoDeEvento(" " + hito.name()).isEmpty());
    }

    @Test
    void tipoNuloOVacioODesconocidoNoTieneHito() {
        assertTrue(HitoDelRecorrido.deTipoDeEvento(null).isEmpty());
        assertTrue(HitoDelRecorrido.deTipoDeEvento("").isEmpty());
        assertTrue(HitoDelRecorrido.deTipoDeEvento("UNKNOWN").isEmpty());
    }
}
