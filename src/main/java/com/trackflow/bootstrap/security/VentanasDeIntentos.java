package com.trackflow.bootstrap.security;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ventanas fijas de conteo en memoria, una por clave. Se usa para el límite de
 * consultas de rastreo: cada petición suma y la ventana se reinicia al cumplirse el
 * plazo. Copiada de la API 1 para no compartir contadores entre instancias ni entre
 * APIs.
 */
final class VentanasDeIntentos {
    private record Ventana(long hasta, long id, int cantidad) { }

    private final Map<String, Ventana> ventanas = new LinkedHashMap<>(16, 0.75f, true);
    private final int capacidad;
    private final long duracionMillis;
    private final Clock clock;
    private long ultimaReserva;

    VentanasDeIntentos(int capacidad, long duracionMillis, Clock clock) {
        this.capacidad = capacidad;
        this.duracionMillis = duracionMillis;
        this.clock = clock;
    }

    synchronized Long reservar(String clave, int limite) {
        long ahora = clock.millis();
        Ventana ventana = ventanas.get(clave);
        if (ventana == null || ventana.hasta() <= ahora) {
            ventanas.entrySet().removeIf(e -> e.getValue().hasta() <= ahora);
            if (ventanas.size() >= capacidad) {
                ventanas.remove(ventanas.keySet().iterator().next());
            }
            long id = ++ultimaReserva;
            ventanas.put(clave, new Ventana(ahora + duracionMillis, id, 1));
            return id;
        }
        if (ventana.cantidad() >= limite) {
            return null;
        }
        ventanas.put(clave, new Ventana(ventana.hasta(), ventana.id(), ventana.cantidad() + 1));
        return ventana.id();
    }

    synchronized void liberar(String clave, Long hasta) {
        Ventana ventana = ventanas.get(clave);
        if (ventana != null && hasta != null && ventana.id() == hasta) {
            if (ventana.cantidad() <= 1) {
                ventanas.remove(clave);
            } else {
                ventanas.put(clave, new Ventana(ventana.hasta(), ventana.id(), ventana.cantidad() - 1));
            }
        }
    }
}
