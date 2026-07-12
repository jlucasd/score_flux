package com.scoreflux.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:modulos-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
)
class ModulosApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void autenticar() {
        TestAuth.autenticar(rest);
    }

    @Test
    void caixa_saldoCorrenteAcumulaOrdenadoPorData() {
        rest.postForObject("/api/caixa",
                Map.of("data", "2026-03-01", "historico", "Venda", "entrada", 1000, "saida", 0, "status", "REALIZADO"),
                JsonNode.class);
        JsonNode extrato = rest.postForObject("/api/caixa",
                Map.of("data", "2026-03-05", "historico", "Pagamento fornecedor", "entrada", 0, "saida", 400, "status", "REALIZADO"),
                JsonNode.class);

        assertThat(extrato.get("linhas")).hasSize(2);
        assertThat(extrato.get("linhas").get(0).get("saldo").asDouble()).isEqualTo(1000.0);
        assertThat(extrato.get("linhas").get(1).get("saldo").asDouble()).isEqualTo(600.0);
        assertThat(extrato.get("saldoFinal").asDouble()).isEqualTo(600.0);
        assertThat(extrato.get("totalEntradas").asDouble()).isEqualTo(1000.0);
        assertThat(extrato.get("totalSaidas").asDouble()).isEqualTo(400.0);
    }

    @Test
    void carteira_saldoAbertoEStatusSemLimite() {
        long clienteId = rest.postForObject("/api/clientes",
                Map.of("nome", "Cliente Carteira", "tipo", "REVENDA"), JsonNode.class).get("id").asLong();

        rest.postForObject("/api/carteira/clientes/" + clienteId + "/movimentos",
                Map.of("data", "2026-03-01", "tipo", "FATURAMENTO", "valor", 1000), JsonNode.class);
        JsonNode carteira = rest.postForObject("/api/carteira/clientes/" + clienteId + "/movimentos",
                Map.of("data", "2026-03-10", "tipo", "PAGAMENTO", "valor", 300), JsonNode.class);

        JsonNode posicao = null;
        for (JsonNode p : carteira.get("posicoes")) {
            if (p.get("clienteId").asLong() == clienteId) posicao = p;
        }
        assertThat(posicao).isNotNull();
        assertThat(posicao.get("saldoAberto").asDouble()).isEqualTo(700.0);
        assertThat(posicao.get("status").asText()).isEqualTo("SEM_LIMITE"); // sem análise concluída
        assertThat(posicao.get("limite").isNull()).isTrue();
    }

    @Test
    void carteira_comLimiteDefineStatusOkOuBloquear() {
        long clienteId = rest.postForObject("/api/clientes",
                Map.of("nome", "Cliente Limite", "tipo", "PRODUTOR"), JsonNode.class).get("id").asLong();

        // Demonstrativo simples só para dar base ao limite
        rest.put("/api/clientes/" + clienteId + "/demonstrativos/2025",
                Map.of("receitaBruta", 200000, "patrimonioLiquido", 200000));

        // Análise nota máxima em tudo → AAA (15%) → limite = média(200k,200k)×15% = 30.000
        long analiseId = rest.postForObject("/api/clientes/" + clienteId + "/analises", null, JsonNode.class)
                .get("id").asLong();
        JsonNode politica = rest.getForObject("/api/politica", JsonNode.class);
        var respostas = new java.util.ArrayList<Map<String, Object>>();
        for (JsonNode sub : politica.get("subcriterios")) {
            long melhor = 0;
            int melhorNota = -1;
            for (JsonNode op : sub.get("opcoes")) {
                if (op.get("nota").asInt() > melhorNota) {
                    melhorNota = op.get("nota").asInt();
                    melhor = op.get("id").asLong();
                }
            }
            respostas.add(Map.of("subcriterioId", sub.get("id").asLong(), "opcaoId", melhor));
        }
        rest.exchange("/api/analises/" + analiseId + "/respostas",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(Map.of("observacoes", "", "respostas", respostas)),
                JsonNode.class);
        rest.postForObject("/api/analises/" + analiseId + "/concluir", null, JsonNode.class);

        // Fatura 25k (dentro do limite de 30k) → OK
        JsonNode carteira = rest.postForObject("/api/carteira/clientes/" + clienteId + "/movimentos",
                Map.of("data", "2026-03-01", "tipo", "FATURAMENTO", "valor", 25000), JsonNode.class);
        JsonNode pos = posicaoDe(carteira, clienteId);
        assertThat(pos.get("limite").asDouble()).isEqualTo(30000.0);
        assertThat(pos.get("disponivel").asDouble()).isEqualTo(5000.0);
        assertThat(pos.get("status").asText()).isEqualTo("OK");

        // Fatura mais 10k → estoura o limite → BLOQUEAR
        JsonNode carteira2 = rest.postForObject("/api/carteira/clientes/" + clienteId + "/movimentos",
                Map.of("data", "2026-03-02", "tipo", "FATURAMENTO", "valor", 10000), JsonNode.class);
        JsonNode pos2 = posicaoDe(carteira2, clienteId);
        assertThat(pos2.get("disponivel").asDouble()).isEqualTo(-5000.0);
        assertThat(pos2.get("status").asText()).isEqualTo("BLOQUEAR");
    }

    @Test
    void parecer_retornaPdf() {
        long clienteId = rest.postForObject("/api/clientes",
                Map.of("nome", "Cliente Parecer", "tipo", "PRODUTOR"), JsonNode.class).get("id").asLong();
        long analiseId = rest.postForObject("/api/clientes/" + clienteId + "/analises", null, JsonNode.class)
                .get("id").asLong();

        ResponseEntity<byte[]> resposta = rest.getForEntity("/api/analises/" + analiseId + "/parecer", byte[].class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getContentType().toString()).contains("application/pdf");
        byte[] corpo = resposta.getBody();
        assertThat(corpo).isNotEmpty();
        // Assinatura de arquivo PDF: %PDF
        assertThat(new String(corpo, 0, 4)).isEqualTo("%PDF");
    }

    private JsonNode posicaoDe(JsonNode carteira, long clienteId) {
        for (JsonNode p : carteira.get("posicoes")) {
            if (p.get("clienteId").asLong() == clienteId) return p;
        }
        throw new AssertionError("posição não encontrada para cliente " + clienteId);
    }
}
