package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.application.EnviosRetrasados.Retraso;
import com.trackflow.modules.reports.domain.ReglaDeRetraso;
import com.trackflow.modules.reports.domain.Severidad;
import com.trackflow.modules.reports.domain.ShipmentTrackingView;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qué envíos llevan detenidos más tiempo del esperado. El tiempo detenido se cuenta
 * desde el último movimiento; si el envío nunca se ha movido, desde su registro.
 * Un envío justo en el límite no se considera retrasado.
 */
@Service
public class ConsultarEnviosRetrasados {

    private final EnviosEnRedRepository repository;
    private final ReglaDeRetraso regla;
    private final Clock clock;

    public ConsultarEnviosRetrasados(EnviosEnRedRepository repository, ReglaDeRetraso regla, Clock clock) {
        this.repository = repository;
        this.regla = regla;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EnviosRetrasados ejecutar() {
        Instant ahora = clock.instant();

        List<Retraso> retrasados = repository.buscarEnRed().stream()
                .map(vista -> evaluar(vista, ahora))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(Retraso::exceso).reversed())
                .toList();

        return new EnviosRetrasados(ahora, retrasados);
    }

    private Optional<Retraso> evaluar(ShipmentTrackingView vista, Instant ahora) {
        Optional<Duration> maximo = regla.maximoPara(vista.getStatus(), vista.getPlannedLegHours());
        if (maximo.isEmpty()) {
            return Optional.empty();
        }

        Instant desde = vista.getLastMovementAt() != null ? vista.getLastMovementAt() : vista.getRegisteredAt();
        Duration detenido = Duration.between(desde, ahora);
        if (detenido.compareTo(maximo.get()) <= 0) {
            return Optional.empty();
        }

        Severidad severidad = Severidad.de(detenido.minus(maximo.get()), maximo.get());
        return Optional.of(new Retraso(vista.getTrackingNumber(), vista.getStatus(), vista.getLastMovementPoint(),
                vista.getOriginCity(), vista.getDestinationCity(), detenido, maximo.get(), severidad,
                vista.getNextStopCity(), vista.getPlannedLegHours()));
    }
}
