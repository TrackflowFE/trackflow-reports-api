package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ProyectarVolumenDeEnvios;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

/**
 * Alimenta la proyección del reporte de volumen con los eventos de shipments y
 * logistics. Las tres proyecciones se confirman juntas antes del ACK del broker.
 */
@Component
public class VolumenProjectionSubscriber {

    private final ProyectarVolumenDeEnvios proyeccion;

    public VolumenProjectionSubscriber(ProyectarVolumenDeEnvios proyeccion) {
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
