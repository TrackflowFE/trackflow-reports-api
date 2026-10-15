package com.trackflow.modules.reports.infrastructure;

import com.trackflow.modules.reports.domain.ReglaDeRetraso;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** La regla del tiempo esperado se puede ajustar en application.properties, sin recompilar. */
@Configuration
public class ReglaDeRetrasoConfig {

    @Bean
    public ReglaDeRetraso reglaDeRetraso(
            @Value("${trackflow.reports.retraso.max-horas.registered:24}") long registrado,
            @Value("${trackflow.reports.retraso.max-horas.at-distribution-center:24}") long enCentro,
            @Value("${trackflow.reports.retraso.max-horas.in-transit:48}") long enTransito,
            @Value("${trackflow.reports.retraso.max-horas.out-for-delivery:12}") long enReparto) {
        return new ReglaDeRetraso(Map.of(
                "REGISTERED", Duration.ofHours(registrado),
                "AT_DISTRIBUTION_CENTER", Duration.ofHours(enCentro),
                "IN_TRANSIT", Duration.ofHours(enTransito),
                "OUT_FOR_DELIVERY", Duration.ofHours(enReparto)));
    }
}
