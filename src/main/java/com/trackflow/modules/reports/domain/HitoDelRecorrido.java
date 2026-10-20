package com.trackflow.modules.reports.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    // El mapa conserva Optional.empty() tambien para tipos de evento nulos.
    private static final Map<String, HitoDelRecorrido> POR_TIPO = Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(
                    HitoDelRecorrido::name, Function.identity(), (first, ignored) -> first, HashMap::new)));

    public static Optional<HitoDelRecorrido> deTipoDeEvento(String tipo) {
        return Optional.ofNullable(POR_TIPO.get(tipo));
    }
}
