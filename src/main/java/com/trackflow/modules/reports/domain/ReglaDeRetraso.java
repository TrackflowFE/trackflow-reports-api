package com.trackflow.modules.reports.domain;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Cuánto tiempo puede estar un envío sin movimiento, según su estado, antes de
 * considerarse retrasado. Los estados que no aparecen (p. ej. DELIVERED) no se vigilan.
 */
public final class ReglaDeRetraso {

    private final Map<String, Duration> maximoSinMovimiento;

    public ReglaDeRetraso(Map<String, Duration> maximoSinMovimiento) {
        this.maximoSinMovimiento = Map.copyOf(maximoSinMovimiento);
    }

    public static ReglaDeRetraso porDefecto() {
        return new ReglaDeRetraso(Map.of(
                "REGISTERED", Duration.ofHours(24),
                "AT_DISTRIBUTION_CENTER", Duration.ofHours(24),
                "IN_TRANSIT", Duration.ofHours(48),
                "OUT_FOR_DELIVERY", Duration.ofHours(12)));
    }

    public Optional<Duration> maximoPara(String status) {
        return Optional.ofNullable(maximoSinMovimiento.get(status));
    }

    /** Margen sobre la duración planeada del tramo: descargas, tráfico, controles. */
    static final double TOLERANCIA_DEL_TRAMO = 1.25;

    /**
     * Como {@link #maximoPara(String)}, pero un envío en camino se compara contra su
     * propio tramo: la duración planeada más un 25 %, redondeada a la hora siguiente. Un
     * Medellín → Bogotá (9 h) espera 12 h, no las mismas 48 h que un Barranquilla →
     * Bogotá (18 h). Sin tramo conocido, o si el envío espera en un hub, rige la regla
     * fija del estado.
     *
     * @param horasDelTramo duración planeada del tramo en curso, o null si no va en camino
     */
    public Optional<Duration> maximoPara(String status, Integer horasDelTramo) {
        if ("IN_TRANSIT".equals(status) && horasDelTramo != null && horasDelTramo > 0) {
            return Optional.of(Duration.ofHours((long) Math.ceil(horasDelTramo * TOLERANCIA_DEL_TRAMO)));
        }
        return maximoPara(status);
    }
}
