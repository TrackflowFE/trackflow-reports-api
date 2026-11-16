package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.application.LimitesDePeriodo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los límites del periodo se ajustan en application.properties, sin recompilar. Una
 * configuración imposible (inicio en el futuro o máximo menor que un día) detiene el
 * arranque: es preferible a servir reportes con límites que no significan nada.
 */
@Configuration
public class LimitesDePeriodoConfig {

    @Bean
    public LimitesDePeriodo limitesDePeriodo(
            @Value("${trackflow.reports.inicio-operacion:2026-01-01}") String inicio,
            @Value("${trackflow.reports.periodo-maximo-dias:366}") int maximoDias,
            Clock clock) {
        LocalDate inicioOperacion;
        try {
            inicioOperacion = LocalDate.parse(inicio);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "El inicio de la operación debe ser una fecha AAAA-MM-DD: " + inicio, e);
        }
        if (inicioOperacion.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException(
                    "El inicio de la operación no puede estar en el futuro: " + inicioOperacion);
        }
        if (maximoDias < 1) {
            throw new IllegalArgumentException(
                    "El periodo máximo debe ser de al menos un día: " + maximoDias);
        }
        return new LimitesDePeriodo(inicioOperacion, maximoDias);
    }
}
