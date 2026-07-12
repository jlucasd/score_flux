package com.scoreflux.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.util.List;
import java.util.Map;

/** Faz login com o admin seed e injeta o Bearer token em todas as chamadas do TestRestTemplate. */
final class TestAuth {

    private TestAuth() {
    }

    static void autenticar(TestRestTemplate rest) {
        rest.getRestTemplate().setInterceptors(List.of());
        JsonNode resposta = rest.postForObject("/api/auth/login",
                Map.of("email", "admin@scoreflux.com", "senha", "admin123"), JsonNode.class);
        String token = resposta.get("token").asText();
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            request.getHeaders().setBearerAuth(token);
            return execution.execute(request, body);
        };
        rest.getRestTemplate().setInterceptors(List.of(interceptor));
    }
}
