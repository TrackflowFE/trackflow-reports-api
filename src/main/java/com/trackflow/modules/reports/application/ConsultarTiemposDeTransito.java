package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.application.TiemposDeTransito.PromedioDeTramo;
import com.trackflow.modules.reports.application.ShipmentTransitViewRepository.Medicion;
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

    public ConsultarTiemposDeTransito(ShipmentTransitViewRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TiemposDeTransito ejecutar(LocalDate desde, LocalDate hasta) {
        PeriodoDeConsulta periodo = PeriodoDeConsulta.resolver(desde, hasta, clock);

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
