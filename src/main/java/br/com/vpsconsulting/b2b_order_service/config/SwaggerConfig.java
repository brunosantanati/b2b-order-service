package br.com.vpsconsulting.b2b_order_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("B2B Order Service API")
                        .version("1.0.0")
                        .description("Microserviço B2B para gestão e processamento de pedidos com integração ao Apache Kafka e MongoDB.")
                        .contact(new Contact()
                                .name("VPS Consulting Tech Team")
                                .email("bruno.santana.ti@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}