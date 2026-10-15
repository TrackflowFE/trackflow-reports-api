package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ProyectarTiemposDeTransito;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

/** Alimenta tiempos de transito en la transaccion del mensaje recibido. */
@Component
public class TiemposProjectionSubscriber {

    private final ProyectarTiemposDeTransito proyeccion;

    public TiemposProjectionSubscriber(ProyectarTiemposDeTransito proyeccion) {
        this.proyeccion = proyeccion;
    }

    @EventListener
    public void on(EnvioCreadoEvent event) {
        proyeccion.alCrearEnvio(event);
    }

    @EventListener
    public void on(EventoLogisticoRegistradoEvent event) {
        proyeccion.alRegistrarEvento(event);
    }
}
