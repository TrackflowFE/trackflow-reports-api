package com.trackflow.modules.reports.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuántos envíos se registraron en un periodo y por qué punto de la red entraron.
 *
 * El periodo se expresa en días calendario de Colombia, que es como piensa la
 * operación: "el 15 de septiembre" va de la medianoche a la medianoche de Bogotá, no
 * de UTC (que lo correría cinco horas). Sin fechas, el periodo es el día en curso, y
 * cuando el periodo incluye hoy se cuenta solo hasta el momento de la consulta.
 */
@Service
public class ConsultarVolumenDeEnvios {

    /** Los puntos con más carga primero; los pendientes de recepción al final. */
    private static final Comparator<VolumenPorPunto> ORDEN = Comparator
            .comparing(VolumenPorPunto::pendienteDeRecepcion)
            .thenComparing(VolumenPorPunto::envios, Comparator.reverseOrder())
            .thenComparing(VolumenPorPunto::punto, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ShipmentVolumeViewRepository repository;
    private final Clock clock;

    public ConsultarVolumenDeEnvios(ShipmentVolumeViewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @param desde primer día, inclusive; si es null, hoy
     * @param hasta último día, inclusive; si es null, el mismo día que {@code desde}
     */
    @Transactional(readOnly = true)
    public VolumenDeEnvios ejecutar(LocalDate desde, LocalDate hasta) {
        PeriodoDeConsulta periodo = PeriodoDeConsulta.resolver(desde, hasta, clock);
        Instant inicio = periodo.inicio();
        Instant corte = periodo.corte();

        List<VolumenPorPunto> porPunto = new ArrayList<>(repository.registradosPorPuntoDeIngreso(inicio, corte));
        porPunto.addAll(repository.registradosPendientesDeRecepcion(inicio, corte));
        porPunto.sort(ORDEN);

        long totalRegistrados = porPunto.stream().mapToLong(VolumenPorPunto::envios).sum();
        long totalEntregados = repository.entregadosEntre(inicio, corte);

        return new VolumenDeEnvios(periodo.desde(), periodo.hasta(), inicio, corte, periodo.enCurso(),
                totalRegistrados, totalEntregados, List.copyOf(porPunto));
    }
}
