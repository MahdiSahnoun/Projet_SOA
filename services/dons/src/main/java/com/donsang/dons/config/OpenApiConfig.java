package com.donsang.dons.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Spécification OpenAPI : /v3/api-docs ; interface : /swagger-ui.html (bouton « Authorize » pour le JWT). */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "API Dons de sang", version = "v1",
                description = "Service orchestrateur : donneurs, dons, demandes hospitalières et stock"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
