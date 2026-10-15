package com.trackflow.modules.reports.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Escenarios de la HU de tiempos de tránsito, con un repositorio en memoria. La consulta
 * SQL real se verifica aparte contra PostgreSQL (ver README).
 */
class ReporteTiemposDeTransitoTest {

    /** Hoy es 10 de octubre de 2026, 12:00 en Colombia (17:00 UTC). */
    private static final Instant AHORA = Instant.parse("2026-10-10T17:00:00Z");
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final EnMemoria repositorio = new EnMemoria();
    private final ProyectarTiemposDeTransito proyeccion = new ProyectarTiemposDeTransito(repositorio);
    private final ConsultarTiemposDeTransito consulta = new ConsultarTiemposDeTransito(repositorio, clock);

    @Test
    void periodoConEntregasCalculaPromedioTotalYPorTramo() {
        // A: 2 h + 20 h + 4 h + 3 h = 29 h.  B: 4 h + 30 h + 2 h + 1 h = 37 h.
        recorrido("A", "2026-10-02T18:00:00Z", 2, 20, 4, 3);
        recorrido("B", "2026-10-02T18:00:00Z", 4, 30, 2, 1);
        // Entregados el 2 de octubre en Colombia.
        LocalDate dia = LocalDate.of(2026, 10, 2);

        TiemposDeTransito r = consulta.ejecutar(dia, dia);

        assertTrue(r.hayDatos());
        assertEquals(2, r.enviosEntregados());
        assertEquals(Duration.ofHours(33), r.promedioTotal());
        assertEquals(List.of(TramoDeTransito.values()),
                r.tramos().stream().map(TiemposDeTransito.PromedioDeTramo::tramo).toList());
        assertEquals(List.of(3L, 25L, 3L, 2L),
                r.tramos().stream().map(t -> t.promedio().toHours()).toList());
        // Los tramos suman el total.
        assertEquals(r.promedioTotal(), r.tramos().stream().map(t -> t.promedio())
                .reduce(Duration.ZERO, Duration::plus));
    }

    @Test
    void soloContarEnviosEntregadosIgnoraLosQueSiguenEnTransito() {
        recorrido("A", "2026-10-02T18:00:00Z", 2, 20, 4, 3);
        // C solo salió del centro de origen: sigue en tránsito.
        evento("C", "RECEIVED_AT_CENTER", "2026-10-01T14:00:00Z");
        evento("C", "DISPATCHED", "2026-10-01T16:00:00Z");
        LocalDate dia = LocalDate.of(2026, 10, 2);

        TiemposDeTransito r = consulta.ejecutar(dia, dia);

        assertEquals(1, r.enviosEntregados());
        assertEquals(Duration.ofHours(29), r.promedioTotal());
    }

    @Test
    void periodoSinEntregasInformaQueNoHayDatos() {
        evento("C", "RECEIVED_AT_CENTER", "2026-10-01T14:00:00Z");
        LocalDate dia = LocalDate.of(2026, 10, 5);

        TiemposDeTransito r = consulta.ejecutar(dia, dia);

        assertFalse(r.hayDatos());
        assertEquals(0, r.enviosEntregados());
        assertNull(r.promedioTotal());
        assertTrue(r.tramos().isEmpty());
    }

    @Test
    void elPeriodoSeFiltraPorFechaDeEntregaEnHoraDeColombia() {
        // Entregado a las 23:30 del 2 de octubre en Colombia = 04:30 UTC del día 3.
        recorrido("A", "2026-10-03T04:30:00Z", 1, 1, 1, 1);

        assertEquals(1, consulta.ejecutar(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 2)).enviosEntregados());
        assertEquals(0, consulta.ejecutar(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 3)).enviosEntregados());
    }

    @Test
    void periodoInvertidoOEnElFuturoSeRechaza() {
        assertThrows(PeriodoInvalidoException.class,
                () -> consulta.ejecutar(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1)));
        assertThrows(PeriodoInvalidoException.class,
                () -> consulta.ejecutar(LocalDate.of(2026, 10, 11), null));
    }

    @Test
    void sinFechasElPeriodoEsHoyYEstaEnCurso() {
        TiemposDeTransito r = consulta.ejecutar(null, null);

        assertEquals(LocalDate.of(2026, 10, 10), r.desde());
        assertEquals(LocalDate.of(2026, 10, 10), r.hasta());
        assertTrue(r.enCurso());
        assertEquals(AHORA, r.corte());
    }

    @Test
    void laProyeccionConservaElHitoMasTempranoYIgnoraOtrosTipos() {
        proyeccion.alCrearEnvio(new com.trackflow.shared.events.EnvioCreadoEvent("A", "REGISTERED", null, 1L, "X",
                null, null, 2L, "Y", AHORA, AHORA));

        evento("A", "DISPATCHED", "2026-10-01T10:00:00Z");
        evento("A", "DISPATCHED", "2026-10-01T12:00:00Z"); // reentrega o segundo despacho: se ignora
        evento("A", "DISPATCHED", "2026-10-01T09:00:00Z"); // llegó tarde pero ocurrió antes: gana
        evento("A", "OTRO_TIPO", "2026-10-01T08:00:00Z");

        ShipmentTransitView vista = repositorio.findByTrackingNumber("A").orElseThrow();
        assertEquals(Instant.parse("2026-10-01T09:00:00Z"), vista.getDispatchedAt());
        assertNull(vista.getReceivedAt());
    }

    // --- utilidades

    /** Entregado en {@code entrega}; las horas son la duración de cada tramo. */
    private void recorrido(String tracking, String entrega, int origen, int transporte, int destino, int reparto) {
        Instant fin = Instant.parse(entrega);
        Instant salidaReparto = fin.minus(Duration.ofHours(reparto));
        Instant llegada = salidaReparto.minus(Duration.ofHours(destino));
        Instant despacho = llegada.minus(Duration.ofHours(transporte));
        Instant recibido = despacho.minus(Duration.ofHours(origen));
        evento(tracking, "RECEIVED_AT_CENTER", recibido.toString());
        evento(tracking, "DISPATCHED", despacho.toString());
        evento(tracking, "ARRIVED_AT_DESTINATION_CENTER", llegada.toString());
        evento(tracking, "OUT_FOR_DELIVERY", salidaReparto.toString());
        evento(tracking, "DELIVERED", fin.toString());
    }

    private void evento(String tracking, String tipo, String movidoEn) {
        if (repositorio.findByTrackingNumber(tracking).isEmpty()) {
            repositorio.save(new ShipmentTransitView(tracking));
        }
        Instant momento = Instant.parse(movidoEn);
        proyeccion.alRegistrarEvento(new EventoLogisticoRegistradoEvent(
                1L, tracking, tipo, null, "Centro", 1L, "Ciudad", momento, AHORA, AHORA));
    }

    /** Repositorio en memoria que agrupa como lo hace la consulta SQL: promedia solo lo que tiene ambos hitos. */
    private static final class EnMemoria implements ShipmentTransitViewRepository {
        private final Map<String, ShipmentTransitView> filas = new HashMap<>();

        @Override
        public ShipmentTransitView save(ShipmentTransitView view) {
            filas.put(view.getTrackingNumber(), view);
            return view;
        }

        @Override
        public Optional<ShipmentTransitView> findByTrackingNumber(String trackingNumber) {
            return Optional.ofNullable(filas.get(trackingNumber));
        }

        private List<ShipmentTransitView> entregadas(Instant desde, Instant hasta) {
            return filas.values().stream()
                    .filter(v -> v.getDeliveredAt() != null && !v.getDeliveredAt().isBefore(desde)
                            && v.getDeliveredAt().isBefore(hasta))
                    .toList();
        }

        @Override
        public long contarEntregadosEntre(Instant desde, Instant hasta) {
            return entregadas(desde, hasta).size();
        }

        @Override
        public Map<String, Medicion> medirEntregadosEntre(Instant desde, Instant hasta) {
            List<ShipmentTransitView> vistas = entregadas(desde, hasta);
            Map<String, Medicion> mediciones = new LinkedHashMap<>();
            mediciones.put(TOTAL, medir(vistas, v -> v.getReceivedAt(), v -> v.getDeliveredAt()));
            for (TramoDeTransito t : TramoDeTransito.values()) {
                mediciones.put(t.codigo(), medir(vistas, v -> v.get(t.desde()), v -> v.get(t.hasta())));
            }
            return mediciones;
        }

        private static Medicion medir(List<ShipmentTransitView> vistas,
                java.util.function.Function<ShipmentTransitView, Instant> inicio,
                java.util.function.Function<ShipmentTransitView, Instant> fin) {
            List<Duration> duraciones = new ArrayList<>();
            for (ShipmentTransitView v : vistas) {
                if (inicio.apply(v) != null && fin.apply(v) != null) {
                    duraciones.add(Duration.between(inicio.apply(v), fin.apply(v)));
                }
            }
            if (duraciones.isEmpty()) {
                return new Medicion(null, 0);
            }
            long millis = duraciones.stream().mapToLong(Duration::toMillis).sum() / duraciones.size();
            return new Medicion(Duration.ofMillis(millis), duraciones.size());
        }
    }
}
