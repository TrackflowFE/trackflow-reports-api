package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.PeriodoInvalidoException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Periodo de un reporte, expresado en días calendario de Colombia: "el 15 de
 * septiembre" va de la medianoche a la medianoche de Bogotá, no de UTC. Sin fechas,
 * es el día en curso; si incluye hoy, se cuenta solo hasta el momento de la consulta.
 *
 * @param desde   primer día, inclusive
 * @param hasta   último día, inclusive
 * @param inicio  instante desde el que se cuenta
 * @param corte   instante hasta el que se cuenta (exclusivo)
 * @param enCurso true si el periodo incluye el día en curso
 */
record PeriodoDeConsulta(LocalDate desde, LocalDate hasta, Instant inicio, Instant corte, boolean enCurso) {

    static final ZoneId ZONA_OPERACION = ZoneId.of("America/Bogota");

    /**
     * @param desde primer día, inclusive; si es null, hoy
     * @param hasta último día, inclusive; si es null, el mismo día que {@code desde}
     */
    static PeriodoDeConsulta resolver(LocalDate desde, LocalDate hasta, Clock clock) {
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(ahora, ZONA_OPERACION);

        LocalDate primerDia = desde == null ? hoy : desde;
        LocalDate ultimoDia = hasta == null ? primerDia : hasta;

        if (ultimoDia.isBefore(primerDia)) {
            throw new PeriodoInvalidoException(
                    "El periodo está invertido: 'to' (%s) es anterior a 'from' (%s)".formatted(ultimoDia, primerDia));
        }
        if (primerDia.isAfter(hoy)) {
            throw new PeriodoInvalidoException(
                    "El periodo empieza en el futuro (%s); hoy es %s y el reporte solo cubre lo que ya ocurrió"
                            .formatted(primerDia, hoy));
        }

        Instant inicio = primerDia.atStartOfDay(ZONA_OPERACION).toInstant();
        Instant finDelPeriodo = ultimoDia.plusDays(1).atStartOfDay(ZONA_OPERACION).toInstant();
        boolean enCurso = !ultimoDia.isBefore(hoy);

        return new PeriodoDeConsulta(primerDia, ultimoDia, inicio, enCurso ? ahora : finDelPeriodo, enCurso);
    }
}
