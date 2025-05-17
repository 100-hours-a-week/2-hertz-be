package com.hertz.hertz_be.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwt = "JWT";

        // 각 controller의 API에서 명시적으로 선언해야 JWT AT 입력 가능
        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwt)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(jwt, securityScheme))
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Hertz API")
                .description("Hertz API 서버의 Swagger 문서입니다.")
                .version("1.0.0");
    }
}

