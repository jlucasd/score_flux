package com.scoreflux.service;

import com.scoreflux.domain.OpcaoResposta;
import com.scoreflux.domain.RespostaAnalise;
import com.scoreflux.domain.Subcriterio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCalculatorTest {

    private final ScoreCalculator calculator = new ScoreCalculator();

    private RespostaAnalise resposta(String peso, int nota) {
        Subcriterio sub = new Subcriterio();
        sub.setPeso(new BigDecimal(peso));
        OpcaoResposta opcao = new OpcaoResposta();
        opcao.setNota(nota);
        RespostaAnalise r = new RespostaAnalise();
        r.setSubcriterio(sub);
        r.setOpcao(opcao);
        return r;
    }

    /** Todos os 14 subcritérios da política SulGesso com a nota indicada. */
    private List<RespostaAnalise> politicaCompleta(int notaTodos) {
        return List.of(
                resposta("0.10", notaTodos), resposta("0.15", notaTodos), resposta("0.30", notaTodos),
                resposta("0.05", notaTodos), resposta("0.05", notaTodos), resposta("0.05", notaTodos),
                resposta("0.05", notaTodos), resposta("0.05", notaTodos), resposta("0.05", notaTodos),
                resposta("0.05", notaTodos),
                resposta("0.025", notaTodos), resposta("0.025", notaTodos),
                resposta("0.025", notaTodos), resposta("0.025", notaTodos));
    }

    @Test
    void notaMaximaEmTudoDaScore100RatingAAA() {
        var r = calculator.calcular(politicaCompleta(100),
                new BigDecimal("500000"), new BigDecimal("1500000"));

        assertThat(r.score()).isEqualByComparingTo("100.00");
        assertThat(r.rating()).isEqualTo("AAA");
        assertThat(r.percentualLimite()).isEqualByComparingTo("0.15");
        // limite = média(500k, 1.5M) × 15% = 1M × 0,15 = 150.000
        assertThat(r.baseLimite()).isEqualByComparingTo("1000000.00");
        assertThat(r.limite()).isEqualByComparingTo("150000.00");
    }

    @Test
    void scorePonderado_exemploManual() {
        // Serasa sem restrições (0,30×100) + conceito bancário com restrições (0,15×20)
        // + conceito comercial bom (0,10×70) = 30 + 3 + 7 = 40 → rating D → limite 0
        var respostas = List.of(
                resposta("0.30", 100), resposta("0.15", 20), resposta("0.10", 70));

        var r = calculator.calcular(respostas, new BigDecimal("100"), new BigDecimal("100"));

        assertThat(r.score()).isEqualByComparingTo("40.00");
        assertThat(r.rating()).isEqualTo("D");
        assertThat(r.percentualLimite()).isEqualByComparingTo("0");
        assertThat(r.limite()).isEqualByComparingTo("0.00");
    }

    @Test
    void semRespostasScoreZeroRatingH() {
        var r = calculator.calcular(List.of(), new BigDecimal("100"), new BigDecimal("100"));

        assertThat(r.score()).isEqualByComparingTo("0.00");
        assertThat(r.rating()).isEqualTo("H");
    }

    @Test
    void fronteirasDaTabelaDeRating() {
        // Limites inferiores exatos de cada faixa (tabela D93:F107 da planilha)
        assertThat(ratingPara("94.6")).isEqualTo("AAA");
        assertThat(ratingPara("94.59")).isEqualTo("AA");
        assertThat(ratingPara("89.2")).isEqualTo("AA");
        assertThat(ratingPara("73")).isEqualTo("B+");
        assertThat(ratingPara("67.6")).isEqualTo("B");
        assertThat(ratingPara("51.4")).isEqualTo("C");
        assertThat(ratingPara("46")).isEqualTo("C-");
        assertThat(ratingPara("45.99")).isEqualTo("D");
        assertThat(ratingPara("9.2")).isEqualTo("G");
        assertThat(ratingPara("9.19")).isEqualTo("H");
    }

    private String ratingPara(String scoreAlvo) {
        // peso 1.0 com nota = score alvo (escala 0-100 → nota inteira ×100/100)
        var r = calculator.calcular(
                List.of(resposta(new BigDecimal(scoreAlvo).divide(new BigDecimal("100")).toPlainString(), 100)),
                null, null);
        return r.rating();
    }

    @Test
    void semDemonstrativoLimiteNull() {
        var r = calculator.calcular(politicaCompleta(100), null, null);

        assertThat(r.rating()).isEqualTo("AAA");
        assertThat(r.baseLimite()).isNull();
        assertThat(r.limite()).isNull();
    }
}
