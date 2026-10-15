package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.ShipmentTransitView;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** Los rangos son semiabiertos, [desde, hasta), como en el reporte de volumen. */
public interface ShipmentTransitViewRepository {

    /** Promedio de un tramo (o del total) y cuántos envíos lo componen. */
    record Medicion(Duration promedio, long envios) {
    }

    /** Clave de {@link #medirEntregadosEntre} para el tiempo total. */
    String TOTAL = "TOTAL";

    ShipmentTransitView save(ShipmentTransitView view);

    Optional<ShipmentTransitView> findByTrackingNumber(String trackingNumber);

    long contarEntregadosEntre(Instant desde, Instant hasta);

    /**
     * Mide solo envíos entregados en el rango. Devuelve una medición por tramo
     * ({@link TramoDeTransito#codigo()}) y una para {@link #TOTAL}; cada una promedia
     * únicamente los envíos que tienen ambos hitos.
     */
    Map<String, Medicion> medirEntregadosEntre(Instant desde, Instant hasta);
}
