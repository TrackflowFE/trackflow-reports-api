package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ProyectarTiemposDeTransito;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Alimenta la proyección de tiempos de tránsito; aparte de las otras para que un fallo no arrastre a las demás. */
@Component
public class TiemposProjectionSubscriber {

    private final ProyectarTiemposDeTransito proyeccion;

    public TiemposProjectionSubscriber(ProyectarTiemposDeTransito proyeccion) {
        this.proyeccion = proyeccion;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EnvioCreadoEvent event) {
        proyeccion.alCrearEnvio(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EventoLogisticoRegistradoEvent event) {
        proyeccion.alRegistrarEvento(event);
    }
}
