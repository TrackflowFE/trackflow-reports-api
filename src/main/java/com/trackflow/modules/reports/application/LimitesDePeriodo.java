package com.trackflow.modules.reports.application;

import java.time.LocalDate;

/**
 * Límites del periodo que aceptan los reportes: no se consulta antes de que empezara
 * la operación y el rango no puede ser tan amplio que el reporte deje de ser útil.
 *
 * @param inicioOperacion primer día con envíos; antes de esa fecha no hay datos
 * @param maximoDias      días calendario que puede abarcar un periodo, contando los dos extremos
 */
public record LimitesDePeriodo(LocalDate inicioOperacion, int maximoDias) {
}
