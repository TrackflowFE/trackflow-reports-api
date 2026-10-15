package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.HitoDelRecorrido;

/**
 * Cada tramo es el lapso entre dos hitos consecutivos del recorrido. La definición de
 * tramo es negociable (ver la HU): cambiarla es cambiar esta lista y la consulta SQL
 * del repositorio, que usa estos mismos códigos.
 */
public enum TramoDeTransito {
    PROCESAMIENTO_ORIGEN("ORIGIN_PROCESSING", "Procesamiento en centro de origen",
            HitoDelRecorrido.RECEIVED_AT_CENTER, HitoDelRecorrido.DISPATCHED),
    TRANSPORTE("TRANSPORT", "Transporte hasta el centro destino",
            HitoDelRecorrido.DISPATCHED, HitoDelRecorrido.ARRIVED_AT_DESTINATION_CENTER),
    PROCESAMIENTO_DESTINO("DESTINATION_PROCESSING", "Procesamiento en centro destino",
            HitoDelRecorrido.ARRIVED_AT_DESTINATION_CENTER, HitoDelRecorrido.OUT_FOR_DELIVERY),
    REPARTO("LAST_MILE", "Reparto al destinatario",
            HitoDelRecorrido.OUT_FOR_DELIVERY, HitoDelRecorrido.DELIVERED);

    private final String codigo;
    private final String nombre;
    private final HitoDelRecorrido desde;
    private final HitoDelRecorrido hasta;

    TramoDeTransito(String codigo, String nombre, HitoDelRecorrido desde, HitoDelRecorrido hasta) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.desde = desde;
        this.hasta = hasta;
    }

    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public HitoDelRecorrido desde() { return desde; }
    public HitoDelRecorrido hasta() { return hasta; }
}
