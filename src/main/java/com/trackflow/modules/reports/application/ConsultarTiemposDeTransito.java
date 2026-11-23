package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.application.TiemposDeTransito.PromedioDeTramo;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuánto tardan en recorrer la red los envíos entregados en un periodo, en total y en
 * cada tramo. Solo cuentan los envíos ya entregados: uno todavía en tránsito no tiene
 * un tiempo de recorrido completo y metería ruido en los promedios.
 *
 * El periodo se interpreta igual que en el reporte de volumen (ver PeriodoDeConsulta) y
 * se filtra por la fecha de ENTREGA, no la de registro.
 */
@Service
public class ConsultarTiemposDeTransito {

    private final ShipmentTransitViewRepository repository;
    private final Clock clock;
    private final LimitesDePeriodo limites;
    private final MeterRegistry metricas;

    public ConsultarTiemposDeTransito(ShipmentTransitViewRepository repository, Clock clock,
            LimitesDePeriodo limites, MeterRegistry metricas) {
        this.repository = repository;
        this.clock = clock;
        this.limites = limites;
        this.metricas = metricas;
    }

    @Transactional(readOnly = true)
    public TiemposDeTransito ejecutar(LocalDate desde, LocalDate hasta) {
        PeriodoDeConsulta periodo = PeriodoDeConsulta.resolver(desde, hasta, clock, limites);
        // Se cuenta una vez validado el periodo: un periodo inválido no es un reporte consultado.
        metricas.counter("trackflow_reportes_consultados_total", "tipo", "tiempos_de_transito").increment();

        long entregados = repository.contarEntregadosEntre(periodo.inicio(), periodo.corte());
        if (entregados == 0) {
            return new TiemposDeTransito(periodo.desde(), periodo.hasta(), periodo.corte(), periodo.enCurso(),
                    0, null, List.of());
        }

        Map<String, Medicion> mediciones = repository.medirEntregadosEntre(periodo.inicio(), periodo.corte());
        Medicion total = mediciones.get(ShipmentTransitViewRepository.TOTAL);
        List<PromedioDeTramo> tramos = Arrays.stream(TramoDeTransito.values())
                .map(tramo -> {
                    Medicion m = mediciones.get(tramo.codigo());
                    if (m == null) {
                        return new PromedioDeTramo(tramo, null, 0);
                    }
                    return new PromedioDeTramo(tramo, m.promedio(), m.envios());
                })
                .toList();

        return new TiemposDeTransito(periodo.desde(), periodo.hasta(), periodo.corte(), periodo.enCurso(),
                entregados, total == null ? null : total.promedio(), tramos);
    }
}
