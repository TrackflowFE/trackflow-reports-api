package com.trackflow.modules.reports.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackflow.modules.reports.domain.ReglaDeRetraso;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReglaDeRetrasoConfigTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(ReglaDeRetrasoConfig.class);

    @Test
    void custodiaUsaVeinticuatroHorasPorDefecto() {
        context.run(c -> assertThat(c.getBean(ReglaDeRetraso.class).maximoPara("IN_CUSTODY"))
                .contains(Duration.ofHours(24)));
        assertThat(ReglaDeRetraso.porDefecto().maximoPara("IN_CUSTODY", 3))
                .contains(Duration.ofHours(24));
    }

    @Test
    void custodiaAceptaUnLimiteConfigurado() {
        context.withPropertyValues("trackflow.reports.retraso.max-horas.in-custody=8")
                .run(c -> assertThat(c.getBean(ReglaDeRetraso.class).maximoPara("IN_CUSTODY"))
                        .contains(Duration.ofHours(8)));
    }
}
