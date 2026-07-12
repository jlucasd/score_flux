package com.scoreflux.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:credito-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
)
class CreditoApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @org.junit.jupiter.api.BeforeEach
    void autenticar() {
        TestAuth.autenticar(rest);
    }

    @Test
    void politicaSeedCarregadaCom14SubcriteriosPesoTotal1() {
        JsonNode politica = rest.getForObject("/api/politica", JsonNode.class);

        assertThat(politica.get("nome").asText()).isEqualTo("Política SulGesso v1");
        assertThat(politica.get("subcriterios")).hasSize(14);

        java.math.BigDecimal somaPesos = java.math.BigDecimal.ZERO;
        for (JsonNode sub : politica.get("subcriterios")) {
            somaPesos = somaPesos.add(sub.get("peso").decimalValue());
        }
        assertThat(somaPesos).isEqualByComparingTo("1");
    }

    @Test
    void fluxoCompleto_clienteDemonstrativoAnaliseScoreLimite() {
        // 1. Cliente
        JsonNode cliente = rest.postForObject("/api/clientes",
                Map.of("nome", "Fazenda Boa Vista", "tipo", "PRODUTOR", "uf", "SC"),
                JsonNode.class);
        long clienteId = cliente.get("id").asLong();

        // 2. Demonstrativos de 2 exercícios (empresa saudável)
        rest.put("/api/clientes/" + clienteId + "/demonstrativos/2024", Map.of(
                "receitaBruta", 1000000, "lucroLiquido", 150000,
                "caixaBancos", 100000, "contasReceber", 200000, "estoques", 80000,
                "imobilizado", 500000, "fornecedores", 90000, "emprestimosCurtoPrazo", 30000,
                "passivoNaoCirculante", 60000, "patrimonioLiquido", 700000));
        rest.put("/api/clientes/" + clienteId + "/demonstrativos/2025", Map.of(
                "receitaBruta", 1200000, "lucroLiquido", 200000,
                "caixaBancos", 150000, "contasReceber", 250000, "estoques", 90000,
                "imobilizado", 520000, "fornecedores", 100000, "emprestimosCurtoPrazo", 20000,
                "passivoNaoCirculante", 50000, "patrimonioLiquido", 840000));

        // 3. Indicadores calculados
        JsonNode ind = rest.getForObject("/api/clientes/" + clienteId + "/indicadores", JsonNode.class);
        assertThat(ind.get("evolucaoVendas").asDouble()).isEqualTo(0.20); // +20% de vendas
        assertThat(ind.get("exercicios")).hasSize(2);

        // 4. Análise em rascunho com sugestões automáticas dos 3.x
        JsonNode analise = rest.postForObject("/api/clientes/" + clienteId + "/analises", null, JsonNode.class);
        long analiseId = analise.get("id").asLong();
        JsonNode sugestoes = analise.get("sugestoes");
        assertThat(sugestoes.size()).isEqualTo(4); // 3.1 a 3.4 sugeridos

        // 5. Responde tudo com a melhor opção de cada subcritério
        JsonNode politica = rest.getForObject("/api/politica", JsonNode.class);
        List<Map<String, Object>> respostas = new ArrayList<>();
        for (JsonNode sub : politica.get("subcriterios")) {
            long melhorOpcao = 0;
            int melhorNota = -1;
            for (JsonNode op : sub.get("opcoes")) {
                if (op.get("nota").asInt() > melhorNota) {
                    melhorNota = op.get("nota").asInt();
                    melhorOpcao = op.get("id").asLong();
                }
            }
            respostas.add(Map.of("subcriterioId", sub.get("id").asLong(),
                    "opcaoId", melhorOpcao, "justificativa", "teste"));
        }
        JsonNode aposRespostas = rest.exchange("/api/analises/" + analiseId + "/respostas",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(Map.of("observacoes", "obs", "respostas", respostas)),
                JsonNode.class).getBody();

        // Score ao vivo: 100 → AAA → limite = média(840k, 1.2M) × 15% = 1.02M × 0,15 = 153k
        assertThat(aposRespostas.get("resultado").get("score").asDouble()).isEqualTo(100.0);
        assertThat(aposRespostas.get("resultado").get("rating").asText()).isEqualTo("AAA");
        assertThat(aposRespostas.get("resultado").get("limite").asDouble()).isEqualTo(153000.0);

        // 6. Conclui — snapshot congelado
        JsonNode concluida = rest.postForObject("/api/analises/" + analiseId + "/concluir", null, JsonNode.class);
        assertThat(concluida.get("status").asText()).isEqualTo("CONCLUIDA");
        assertThat(concluida.get("resultado").get("rating").asText()).isEqualTo("AAA");

        // 7. Análise concluída não aceita mais respostas
        ResponseEntity<String> bloqueada = rest.exchange("/api/analises/" + analiseId + "/respostas",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(Map.of("observacoes", "x", "respostas", List.of())),
                String.class);
        assertThat(bloqueada.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
