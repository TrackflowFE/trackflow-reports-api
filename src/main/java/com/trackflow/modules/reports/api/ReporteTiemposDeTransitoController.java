package com.trackflow.modules.reports.api;

import com.trackflow.modules.reports.api.dto.TiemposDeTransitoResponse;
import com.trackflow.modules.reports.application.ConsultarTiemposDeTransito;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reporte de tiempos de tránsito para el supervisor de operaciones: cuánto tardan los
 * envíos entregados en recorrer la red y en qué tramos se pierde el tiempo.
 *
 * Un periodo sin entregas responde 200 con cero envíos y un mensaje, no 404: el reporte
 * existe y su resultado es "no hay datos", igual que en el de volumen.
 */
@RestController
@RequestMapping("/api/reports")
public class ReporteTiemposDeTransitoController {

    private final ConsultarTiemposDeTransito consultarTiempos;

    public ReporteTiemposDeTransitoController(ConsultarTiemposDeTransito consultarTiempos) {
        this.consultarTiempos = consultarTiempos;
    }

    /**
     * @param desde primer día del periodo (AAAA-MM-DD, hora de Colombia); por defecto, hoy
     * @param hasta último día del periodo, inclusive; por defecto, el mismo día que desde
     */
    @GetMapping("/transit-times")
    public TiemposDeTransitoResponse tiempos(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {
        return TiemposDeTransitoResponse.from(consultarTiempos.ejecutar(desde, hasta));
    }
}
