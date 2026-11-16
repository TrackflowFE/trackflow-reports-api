package com.trackflow.modules.reports.api.dto;

import com.trackflow.modules.reports.application.LimitesDePeriodo;
import java.time.LocalDate;

/** Lo que el formulario necesita para no ofrecer un periodo que el servidor va a rechazar. */
public record LimitesDePeriodoResponse(LocalDate minDate, int maxDays) {

    public static LimitesDePeriodoResponse from(LimitesDePeriodo limites) {
        return new LimitesDePeriodoResponse(limites.inicioOperacion(), limites.maximoDias());
    }
}
