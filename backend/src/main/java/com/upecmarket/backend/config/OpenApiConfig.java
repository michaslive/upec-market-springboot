package com.upecmarket.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
	@Bean
	OpenAPI upecMarketOpenApi() {
		String schemeName = "Bearer Authentication";
		return new OpenAPI()
				.info(new Info().title("UPEC Market API")
						.description("API REST para el marketplace universitario UPEC.")
						.version("1.0.0"))
				.addSecurityItem(new SecurityRequirement().addList(schemeName))
				.components(new Components().addSecuritySchemes(schemeName,
						new SecurityScheme().name(schemeName).type(SecurityScheme.Type.HTTP).scheme("bearer")));
	}
}
