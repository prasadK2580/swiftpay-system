package com.swiftpay.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ledgerOpenApi(
            @Value("${app.openapi.server-url:}") String configuredServerUrl,
            @Value("${server.port:8081}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Ledger (Service B)")
                        .version("1.0")
                        .description("Balance, history, and Kafka settlement for payment.initiated events.")
                        .contact(new Contact().name("SwiftPay")))
                .addServersItem(new Server()
                        .url(resolveServerUrl(configuredServerUrl, serverPort))
                        .description("Ledger API"));
    }

    static String resolveServerUrl(String configuredServerUrl, int serverPort) {
        if (StringUtils.hasText(configuredServerUrl)) {
            return configuredServerUrl;
        }
        return "http://localhost:" + serverPort;
    }
}
