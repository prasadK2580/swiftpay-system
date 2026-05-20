package com.swiftpay.gateway.config;

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
    public OpenAPI gatewayOpenApi(
            @Value("${app.openapi.server-url:}") String configuredServerUrl,
            @Value("${server.port:8080}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay Gateway (Service A)")
                        .version("1.0")
                        .description("POST /v1/payments with Idempotency-Key; consumes payment.completed/failed from Kafka.")
                        .contact(new Contact().name("SwiftPay")))
                .addServersItem(new Server()
                        .url(resolveServerUrl(configuredServerUrl, serverPort))
                        .description("Gateway API"));
    }

    /**
     * Local dev: http://localhost:port when unset. Prod/Docker: set APP_OPENAPI_SERVER_URL
     * to public URL or "/" for same-origin behind ingress.
     */
    static String resolveServerUrl(String configuredServerUrl, int serverPort) {
        if (StringUtils.hasText(configuredServerUrl)) {
            return configuredServerUrl;
        }
        return "http://localhost:" + serverPort;
    }
}
