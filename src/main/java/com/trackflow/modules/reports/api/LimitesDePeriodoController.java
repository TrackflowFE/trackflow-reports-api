package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.LimitesDePeriodoResponse;
import com.trackflow.modules.reports.application.LimitesDePeriodo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Límites del periodo para el formulario de reportes. Queda detrás de la regla de
 * /api/reports/**, así que exige rol OPERADOR o ADMIN como los reportes.
 */
@RestController
@RequestMapping("/api/reports")
public class LimitesDePeriodoController {

    private final LimitesDePeriodo limites;

    public LimitesDePeriodoController(LimitesDePeriodo limites) {
        this.limites = limites;
    }

    @GetMapping("/period-limits")
    public LimitesDePeriodoResponse limites() {
        return LimitesDePeriodoResponse.from(limites);
    }
}
