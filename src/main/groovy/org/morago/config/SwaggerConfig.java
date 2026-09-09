package org.morago.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()

                .addSecurityItem(
                        new SecurityRequirement().addList("Bearer Authentication")
                )

                .components(
                        new Components()

                                .addSecuritySchemes(
                                        "Bearer Authentication",

                                        new SecurityScheme()

                                                .type(SecurityScheme.Type.HTTP)

                                                .scheme("bearer")

                                                .bearerFormat("JWT")
                                )
                )
                .info(new Info()
                .title("Morago API")
                .description("Backend для приложения для оказания услуг переводчиков: звонки, отзывы, транзакции, вывод средств, администрирование")
                .version("1.0.0"));
    }
}