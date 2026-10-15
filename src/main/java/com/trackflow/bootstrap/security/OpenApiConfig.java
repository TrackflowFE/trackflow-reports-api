package com.trackflow.bootstrap.security;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el esquema de token en la documentación, para que Swagger muestre el botón
 * "Authorize" y se puedan probar los endpoints protegidos desde el navegador.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "TokenDeOperador";

    @Bean
    OpenAPI trackflowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TrackFlow Reports API")
                        .version("v1")
                        .description("""
                                API 2: rastreo publico y reportes de tiempos, retrasos y volumen.
                                Los reportes requieren el token emitido por el login de API 1.
                                Las proyecciones se alimentan exclusivamente de RabbitMQ."""))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Sin este requisito, el botón Authorize no adjunta el token a las peticiones.
                // Los endpoints públicos siguen funcionando sin él: quien decide es Spring Security.
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
