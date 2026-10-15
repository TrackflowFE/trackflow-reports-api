package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.HitoDelRecorrido;
import com.trackflow.modules.reports.domain.ShipmentTransitView;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantiene el modelo de lectura del reporte de tiempos de tránsito a partir de los
 * eventos de shipments y logistics. Usa la hora en que ocurrió el movimiento
 * ({@code movedAt}), no la de su registro.
 */
@Service
public class ProyectarTiemposDeTransito {

    private final ShipmentTransitViewRepository repository;

    public ProyectarTiemposDeTransito(ShipmentTransitViewRepository repository) {
        this.repository = repository;
    }

    /** Si el envío ya está proyectado no se toca: la reconstrucción republica todas las altas. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCrearEnvio(EnvioCreadoEvent event) {
        if (repository.findByTrackingNumber(event.trackingNumber()).isEmpty()) {
            repository.save(new ShipmentTransitView(event.trackingNumber()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alRegistrarEvento(EventoLogisticoRegistradoEvent event) {
        Optional<HitoDelRecorrido> hito = HitoDelRecorrido.deTipoDeEvento(event.eventType());
        if (hito.isEmpty()) {
            return;
        }

        repository.findByTrackingNumber(event.trackingNumber()).ifPresent(view -> {
            if (view.registrar(hito.get(), event.movedAt())) {
                repository.save(view);
            }
        });
    }
}
