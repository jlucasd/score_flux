package com.scoreflux.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:parametros-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
)
class ParametrosApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void autenticar() {
        TestAuth.autenticar(rest);
    }

    @Test
    void politica_devolveParametrosDoSubcriterio() {
        JsonNode politica = rest.getForObject("/api/politica", JsonNode.class);

        JsonNode sub11 = null;
        for (JsonNode sub : politica.get("subcriterios")) {
            if (sub.get("codigo").asText().equals("1.1")) sub11 = sub;
        }
        assertThat(sub11).isNotNull();
        assertThat(sub11.get("instrumento").asText()).isEqualTo("Relato de Campo");
        assertThat(sub11.get("validacao").asText()).isEqualTo("Comercial");
        assertThat(sub11.get("fonte").asText()).isEqualTo("RTVs");
        assertThat(sub11.get("descricao").asText())
                .isEqualTo("Relato comercial baseado em informações colhidas no campo");
    }

    @Test
    void faixas_devolveTabelaDeRatingDecrescente() {
        JsonNode faixas = rest.getForObject("/api/politica/faixas", JsonNode.class);

        assertThat(faixas).hasSize(15);
        assertThat(faixas.get(0).get("rating").asText()).isEqualTo("AAA");
        assertThat(faixas.get(0).get("percentualLimite").asDouble()).isEqualTo(0.15);
        assertThat(faixas.get(0).get("scoreMinimo").asDouble()).isEqualTo(94.6);
        assertThat(faixas.get(14).get("rating").asText()).isEqualTo("H");
    }

    @Test
    void relatoCampo_fluxoCompletoComUpsert() {
        long clienteId = rest.postForObject("/api/clientes",
                Map.of("nome", "Cliente Relato", "tipo", "REVENDA"), JsonNode.class).get("id").asLong();

        // GET antes de salvar: campos null, cliente preenchido do cadastro
        JsonNode vazio = rest.getForObject("/api/clientes/" + clienteId + "/relato-campo", JsonNode.class);
        assertThat(vazio.get("clienteId").asLong()).isEqualTo(clienteId);
        assertThat(vazio.get("clienteNome").asText()).isEqualTo("Cliente Relato");
        assertThat(vazio.get("conceitoComercial").isNull()).isTrue();
        assertThat(vazio.get("possuiErp").isNull()).isTrue();
        assertThat(vazio.get("atualizadoEm").isNull()).isTrue();

        // PUT cria o relato
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("conceitoComercial", "Excelente");
        corpo.put("possuiErp", true);
        corpo.put("observacoes", "teste");
        JsonNode salvo = rest.exchange("/api/clientes/" + clienteId + "/relato-campo",
                HttpMethod.PUT, new HttpEntity<>(corpo), JsonNode.class).getBody();
        assertThat(salvo.get("conceitoComercial").asText()).isEqualTo("Excelente");
        assertThat(salvo.get("possuiErp").asBoolean()).isTrue();
        assertThat(salvo.get("observacoes").asText()).isEqualTo("teste");
        assertThat(salvo.get("atualizadoEm").isNull()).isFalse();

        // GET devolve o que foi salvo
        JsonNode lido = rest.getForObject("/api/clientes/" + clienteId + "/relato-campo", JsonNode.class);
        assertThat(lido.get("conceitoComercial").asText()).isEqualTo("Excelente");
        assertThat(lido.get("possuiErp").asBoolean()).isTrue();
        assertThat(lido.get("observacoes").asText()).isEqualTo("teste");
        assertThat(lido.get("clienteNome").asText()).isEqualTo("Cliente Relato");

        // Segundo PUT altera (upsert — continua um registro só)
        corpo.put("conceitoComercial", "Bom");
        JsonNode atualizado = rest.exchange("/api/clientes/" + clienteId + "/relato-campo",
                HttpMethod.PUT, new HttpEntity<>(corpo), JsonNode.class).getBody();
        assertThat(atualizado.get("conceitoComercial").asText()).isEqualTo("Bom");

        JsonNode relido = rest.getForObject("/api/clientes/" + clienteId + "/relato-campo", JsonNode.class);
        assertThat(relido.get("conceitoComercial").asText()).isEqualTo("Bom");
        assertThat(relido.get("possuiErp").asBoolean()).isTrue();
    }
}
