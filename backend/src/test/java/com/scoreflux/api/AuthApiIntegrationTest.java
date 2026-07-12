package com.scoreflux.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:auth-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
)
class AuthApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void limparInterceptors() {
        rest.getRestTemplate().setInterceptors(List.of());
        // Apache HttpClient: o HttpURLConnection padrão não lê corpo de 401 em POST
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
    }

    @Test
    void semTokenRecebe401() {
        ResponseEntity<String> resposta = rest.getForEntity("/api/planos", String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void senhaErradaRecebe401() {
        ResponseEntity<String> resposta = rest.postForEntity("/api/auth/login",
                Map.of("email", "admin@scoreflux.com", "senha", "senha-errada"), String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginValidoRetornaTokenQueDaAcesso() {
        JsonNode login = rest.postForObject("/api/auth/login",
                Map.of("email", "admin@scoreflux.com", "senha", "admin123"), JsonNode.class);
        assertThat(login.get("nome").asText()).isEqualTo("Administrador");
        String token = login.get("token").asText();

        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> planos = rest.exchange("/api/planos",
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertThat(planos.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void criarUsuarioELogarComEle() {
        TestAuth.autenticar(rest);
        ResponseEntity<JsonNode> criado = rest.postForEntity("/api/usuarios",
                Map.of("nome", "Analista", "email", "analista@scoreflux.com", "senha", "analista1"),
                JsonNode.class);
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        rest.getRestTemplate().setInterceptors(List.of());
        JsonNode login = rest.postForObject("/api/auth/login",
                Map.of("email", "analista@scoreflux.com", "senha", "analista1"), JsonNode.class);
        assertThat(login.get("token").asText()).isNotBlank();
    }

    @Test
    void emailDuplicadoRecebe400() {
        TestAuth.autenticar(rest);
        ResponseEntity<String> resposta = rest.postForEntity("/api/usuarios",
                Map.of("nome", "Duplicado", "email", "admin@scoreflux.com", "senha", "123456"),
                String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
