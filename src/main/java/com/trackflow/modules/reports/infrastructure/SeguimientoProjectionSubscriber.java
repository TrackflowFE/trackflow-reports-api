package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.ProyectarSeguimientoEnvio;
import com.trackflow.shared.events.EnvioCreadoEvent;
import com.trackflow.shared.events.EventoLogisticoRegistradoEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

/**
 * Alimenta la proyección de seguimiento con los eventos de shipments y logistics.
 *
 * Es una sola clase a propósito: logistics y shipments ya tienen sus propios
 * EnvioCreadoSubscriber y EventoLogisticoSubscriber, y Spring deriva el nombre del
 * bean del nombre simple de la clase, así que repetirlos aquí impide arrancar.
 */
@Component
public class SeguimientoProjectionSubscriber {

    private final ProyectarSeguimientoEnvio proyeccion;

    public SeguimientoProjectionSubscriber(ProyectarSeguimientoEnvio proyeccion) {
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
