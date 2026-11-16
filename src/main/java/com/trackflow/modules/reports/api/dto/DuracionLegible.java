package com.trackflow.modules.reports.api.dto;

import java.time.Duration;

/**
 * Textos de duración para los reportes. Hay dos reglas porque miden cosas distintas:
 * un retraso se lee en días y horas, mientras que un promedio de tránsito necesita
 * minutos para no perder precisión.
 */
public final class DuracionLegible {

    private DuracionLegible() {
    }

    /**
     * "86 d 4 h", "2 d", "23 h" o "menos de 1 h". Los minutos no se muestran: en un
     * retraso de días no aportan y en uno de horas el operador actúa por horas.
     */
    public static String diasYHoras(Duration duracion) {
        long dias = duracion.toDays();
        long horas = duracion.toHoursPart();
        if (dias > 0) {
            return horas > 0 ? dias + " d " + horas + " h" : dias + " d";
        }
        if (horas > 0) {
            return horas + " h";
        }
        return "menos de 1 h";
    }

    /** "1 d 4 h 30 min"; omite las unidades en cero y nunca devuelve vacío. */
    public static String conMinutos(Duration duracion) {
        long[] valores = {duracion.toDays(), duracion.toHoursPart(), duracion.toMinutesPart()};
        String[] unidades = {" d ", " h ", " min "};
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] > 0) texto.append(valores[i]).append(unidades[i]);
        }
        return texto.isEmpty() ? "menos de 1 min" : texto.toString().trim();
    }
}
