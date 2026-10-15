package com.trackflow.modules.reports.application;

import com.trackflow.modules.reports.domain.Severidad;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Envíos que llevan más tiempo detenido del esperado, del más al menos atrasado. */
public record EnviosRetrasados(Instant corte, List<Retraso> envios) {

    /**
     * @param enCaminoA hacia dónde va el camión si el envío está en camino; null si no
     * @param horasDelTramo duración planeada del tramo en curso, si con ella se calculó el
     *        tiempo esperado; null si se usó la regla fija del estado
     */
    public record Retraso(String trackingNumber, String estado, String ultimoPunto, String ciudadOrigen,
            String ciudadDestino, Duration detenido, Duration maximoEsperado, Severidad severidad,
            String enCaminoA, Integer horasDelTramo) {

        public Duration exceso() {
            return detenido.minus(maximoEsperado);
        }
    }

    public boolean hayDatos() {
        return !envios.isEmpty();
    }
}
