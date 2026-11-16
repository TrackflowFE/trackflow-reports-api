package com.trackflow.modules.reports.api.dto;

/**
 * Nombre parcial para el rastreo público: la inicial de cada palabra y tres asteriscos.
 * "Luis Gómez" queda "L*** G***". No se conserva la longitud real, para no dar pistas
 * sobre el nombre completo.
 */
public final class NombreEnmascarado {

    private static final String OCULTO = "***";

    private NombreEnmascarado() {
    }

    public static String de(String nombre) {
        if (nombre == null) {
            return null;
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return "";
        }
        StringBuilder resultado = new StringBuilder();
        for (String palabra : limpio.split(" ")) {
            if (resultado.length() > 0) {
                resultado.append(' ');
            }
            resultado.append(palabra.charAt(0)).append(OCULTO);
        }
        return resultado.toString();
    }
}
