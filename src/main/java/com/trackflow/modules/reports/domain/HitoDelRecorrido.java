package com.trackflow.modules.reports.domain;

import java.util.Optional;

/**
 * Movimientos del recorrido que delimitan los tramos. Los nombres coinciden con el
 * tipo de evento que viaja en el contrato de integración: reports no puede importar el
 * enum de logistics (regla del monolito modular), así que los reconoce por nombre.
 */
public enum HitoDelRecorrido {
    RECEIVED_AT_CENTER,
    DISPATCHED,
    ARRIVED_AT_DESTINATION_CENTER,
    OUT_FOR_DELIVERY,
    DELIVERED;

    public static Optional<HitoDelRecorrido> deTipoDeEvento(String tipo) {
        for (HitoDelRecorrido hito : values()) {
            if (hito.name().equals(tipo)) {
                return Optional.of(hito);
            }
        }
        return Optional.empty();
    }
}
