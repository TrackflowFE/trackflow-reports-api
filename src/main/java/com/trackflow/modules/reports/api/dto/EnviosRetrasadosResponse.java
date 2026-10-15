package com.trackflow.modules.reports.api.dto;

import com.trackflow.modules.reports.application.EnviosRetrasados;
import com.trackflow.modules.reports.domain.Severidad;
import java.time.Instant;
import java.util.List;

public record EnviosRetrasadosResponse(
        /** Instante con el que se midió el retraso. */
        Instant cutoff,

        /** Envíos que llevan detenidos más del tiempo esperado. */
        int totalDelayed,

        /** Solo viene informado cuando no hay envíos retrasados; si no, es null. */
        String message,

        /** Del más al menos atrasado. */
        List<ShipmentResponse> shipments) {

    static final String SIN_RETRASOS = "No hay envíos retrasados en este momento";

    /**
     * lastPoint es null si el envío nunca ha tenido un movimiento. nextStop dice hacia dónde
     * va el camión si el envío está en camino (si no, null), y plannedLegHours es la
     * duración del tramo con la que se calculó hoursExpected (null si se usó la regla fija
     * del estado).
     */
    public record ShipmentResponse(String trackingNumber, String status, String lastPoint, String originCity,
            String destinationCity, long hoursStopped, long hoursExpected, long hoursOver, String severity,
            String nextStop, Integer plannedLegHours) {

        static ShipmentResponse from(EnviosRetrasados.Retraso retraso) {
            return new ShipmentResponse(retraso.trackingNumber(), retraso.estado(), retraso.ultimoPunto(),
                    retraso.ciudadOrigen(), retraso.ciudadDestino(), retraso.detenido().toHours(),
                    retraso.maximoEsperado().toHours(), retraso.exceso().toHours(), severidad(retraso.severidad()),
                    retraso.enCaminoA(), retraso.horasDelTramo());
        }

        private static String severidad(Severidad severidad) {
            return switch (severidad) {
                case LEVE -> "LOW";
                case MODERADA -> "MODERATE";
                case CRITICA -> "CRITICAL";
            };
        }
    }

    public static EnviosRetrasadosResponse from(EnviosRetrasados reporte) {
        return new EnviosRetrasadosResponse(
                reporte.corte(),
                reporte.envios().size(),
                reporte.hayDatos() ? null : SIN_RETRASOS,
                reporte.envios().stream().map(ShipmentResponse::from).toList());
    }
}
