package com.avc.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI avcOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Video Creator API")
                        .description("Spring Boot + Spring AI powered video creation platform")
                        .version("1.0.0"));
    }
}
