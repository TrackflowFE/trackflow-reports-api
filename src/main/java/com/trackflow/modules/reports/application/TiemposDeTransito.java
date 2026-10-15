package com.trackflow.modules.reports.application;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado del reporte de tiempos de tránsito de un periodo.
 *
 * @param desde            primer día del periodo (inclusive), en hora de Colombia
 * @param hasta            último día del periodo (inclusive), en hora de Colombia
 * @param corte            instante hasta el que se cuenta (exclusivo)
 * @param enCurso          true si el periodo incluye el día en curso
 * @param enviosEntregados envíos entregados en el periodo; son los únicos que se miden
 * @param promedioTotal    de que el envío entra a la red a que se entrega; null sin datos
 * @param tramos           promedio de cada tramo; vacío si no hay envíos entregados
 */
public record TiemposDeTransito(
        LocalDate desde,
        LocalDate hasta,
        Instant corte,
        boolean enCurso,
        long enviosEntregados,
        Duration promedioTotal,
        List<PromedioDeTramo> tramos) {

    /**
     * @param promedio null si ningún envío del periodo tiene ambos hitos del tramo
     * @param envios   cuántos envíos entraron en el promedio
     */
    public record PromedioDeTramo(TramoDeTransito tramo, Duration promedio, long envios) {
    }

    public boolean hayDatos() {
        return enviosEntregados > 0;
    }
}
