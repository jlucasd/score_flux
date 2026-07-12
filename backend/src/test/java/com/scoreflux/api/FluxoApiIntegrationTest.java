package com.scoreflux.api;

import com.scoreflux.api.dto.ResumoPlanoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:scoreflux-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
)
class FluxoApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @org.junit.jupiter.api.BeforeEach
    void autenticar() {
        TestAuth.autenticar(rest);
    }

    @Test
    void fluxoCompleto_criarPlanoItemValoresEConferirResumo() {
        var plano = rest.postForEntity("/api/planos",
                Map.of("nome", "Fluxo de Pagamentos", "ano", 2026),
                PlanoController.PlanoResponse.class);
        assertThat(plano.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long planoId = plano.getBody().id();

        var item = rest.postForEntity("/api/planos/" + planoId + "/itens",
                Map.of("descricao", "01 Trator", "mecanismo", "BOLETO", "nota", "Vencimento todo dia 20"),
                ItemController.ItemResponse.class);
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long itemId = item.getBody().id();

        rest.put("/api/itens/" + itemId + "/valores",
                Map.of("2", new BigDecimal("1000.00"), "3", new BigDecimal("2500.00")));

        rest.postForEntity("/api/planos/" + planoId + "/orcamentos",
                Map.of("descricao", "Escavadeira", "valorUnitario", new BigDecimal("450000.00"), "quantidade", 2),
                OrcamentoController.OrcamentoResponse.class);

        ResponseEntity<ResumoPlanoDTO> resumo =
                rest.getForEntity("/api/planos/" + planoId + "/resumo", ResumoPlanoDTO.class);

        assertThat(resumo.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResumoPlanoDTO corpo = resumo.getBody();
        assertThat(corpo.itens()).hasSize(1);
        assertThat(corpo.itens().get(0).total()).isEqualByComparingTo("3500.00");
        assertThat(corpo.totaisPorMes().get(2)).isEqualByComparingTo("1000.00");
        assertThat(corpo.totaisPorMes().get(3)).isEqualByComparingTo("2500.00");
        assertThat(corpo.totalGeral()).isEqualByComparingTo("3500.00");
        assertThat(corpo.totalOrcamentos()).isEqualByComparingTo("900000.00");
    }

    @Test
    void resumoDePlanoInexistenteRetorna404() {
        ResponseEntity<String> resposta = rest.getForEntity("/api/planos/99999/resumo", String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
