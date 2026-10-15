package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.EnviosRetrasadosResponse;
import com.trackflow.modules.reports.application.ConsultarEnviosRetrasados;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte de envíos retrasados para el supervisor de operaciones: los que llevan detenidos
 * más tiempo del esperado, mientras siguen en la red. Si no hay ninguno responde 200 con
 * cero envíos y un mensaje, igual que los otros reportes.
 */
@RestController
@RequestMapping("/api/reports")
public class ReporteEnviosRetrasadosController {

    private final ConsultarEnviosRetrasados consultar;

    public ReporteEnviosRetrasadosController(ConsultarEnviosRetrasados consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/delayed-shipments")
    public EnviosRetrasadosResponse retrasados() {
        return EnviosRetrasadosResponse.from(consultar.ejecutar());
    }
}
