package com.hertz.hertz_be.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url}") // 환경에 따라 다르게 주입됨
    private String swaggerServerUrl;

    @Bean
    public OpenAPI openAPI() {
        String jwt = "JWT";

        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .addServersItem(new Server().url(swaggerServerUrl).description("API 서버")) // 동적 서버 주소
                .components(new Components().addSecuritySchemes(jwt, securityScheme))
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(jwt));
    }

    private Info apiInfo() {
        return new Info()
                .title("Hertz API")
                .description("Hertz API 서버의 Swagger 문서입니다.")
                .version("1.0.0");
    }
}
