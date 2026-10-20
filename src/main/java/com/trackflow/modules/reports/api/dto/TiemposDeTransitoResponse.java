package com.trackflow.modules.reports.api.dto;

import com.trackflow.modules.reports.application.TiemposDeTransito;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TiemposDeTransitoResponse(
        LocalDate from,
        LocalDate to,

        /** Instante hasta el que se contó. */
        Instant cutoff,

        /** true si el periodo incluye el día en curso y la cifra aún puede cambiar. */
        boolean inProgress,

        /** Envíos entregados en el periodo: los únicos que entran en los promedios. */
        long deliveredShipments,

        /** Tiempo promedio de que el envío entra a la red a que se entrega; null sin datos. */
        Promedio averageTotal,

        /** Promedio de cada tramo, en orden de recorrido; vacío sin datos. */
        List<TramoResponse> segments,

        /** Solo viene informado cuando no hubo envíos entregados en el periodo; si no, es null. */
        String message) {

    static final String SIN_DATOS = "No hay datos para el periodo consultado: no se entregó ningún envío";

    public record Promedio(long seconds, String text) {

        static Promedio of(Duration duracion) {
            return duracion == null ? null : new Promedio(duracion.toSeconds(), formatear(duracion));
        }
    }

    public record TramoResponse(String code, String name, String fromEvent, String toEvent, Promedio average,
            long shipmentsMeasured) {
    }

    public static TiemposDeTransitoResponse from(TiemposDeTransito tiempos) {
        return new TiemposDeTransitoResponse(
                tiempos.desde(),
                tiempos.hasta(),
                tiempos.corte(),
                tiempos.enCurso(),
                tiempos.enviosEntregados(),
                Promedio.of(tiempos.promedioTotal()),
                tiempos.tramos().stream()
                        .map(t -> new TramoResponse(t.tramo().codigo(), t.tramo().nombre(),
                                t.tramo().desde().name(), t.tramo().hasta().name(),
                                Promedio.of(t.promedio()), t.envios()))
                        .toList(),
                tiempos.hayDatos() ? null : SIN_DATOS);
    }

    /** "1 d 4 h 30 min"; omite las unidades en cero y nunca devuelve vacío. */
    static String formatear(Duration duracion) {
        long[] valores = {duracion.toDays(), duracion.toHoursPart(), duracion.toMinutesPart()};
        String[] unidades = {" d ", " h ", " min "};
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] > 0) texto.append(valores[i]).append(unidades[i]);
        }
        return texto.isEmpty() ? "menos de 1 min" : texto.toString().trim();
    }
}
