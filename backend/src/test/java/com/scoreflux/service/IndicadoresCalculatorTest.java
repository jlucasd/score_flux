package com.scoreflux.service;

import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.domain.Demonstrativo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresCalculatorTest {

    private final IndicadoresCalculator calculator = new IndicadoresCalculator();

    private Demonstrativo demonstrativo(int exercicio) {
        Demonstrativo d = new Demonstrativo();
        d.setExercicio(exercicio);
        return d;
    }

    @Test
    void indicadoresClassicos() {
        Demonstrativo d = demonstrativo(2025);
        d.setReceitaBruta(new BigDecimal("1000000"));
        d.setLucroLiquido(new BigDecimal("120000"));
        d.setPatrimonioLiquido(new BigDecimal("600000"));
        d.setCaixaBancos(new BigDecimal("50000"));
        d.setContasReceber(new BigDecimal("200000"));
        d.setEstoques(new BigDecimal("100000"));
        d.setFornecedores(new BigDecimal("150000"));
        d.setEmprestimosCurtoPrazo(new BigDecimal("50000"));
        d.setImobilizado(new BigDecimal("400000"));
        d.setPassivoNaoCirculante(new BigDecimal("50000"));

        IndicadoresDTO r = calculator.calcular(List.of(d));
        IndicadoresDTO.ExercicioDTO ex = r.exercicios().get(0);

        // ROE = 120.000 / 600.000 = 0,20
        assertThat(ex.roe()).isEqualByComparingTo("0.2000");
        // AC = 50k+200k+100k = 350k; PC = 150k+50k = 200k; AT = 350k + 400k = 750k
        assertThat(ex.ativoCirculante()).isEqualByComparingTo("350000");
        assertThat(ex.passivoCirculante()).isEqualByComparingTo("200000");
        assertThat(ex.ativoTotal()).isEqualByComparingTo("750000");
        // Endividamento = (200k + 50k) / 750k = 0,3333
        assertThat(ex.endividamento()).isEqualByComparingTo("0.3333");
        // Liquidez seca = (350k − 100k) / 200k = 1,25
        assertThat(ex.liquidezSeca()).isEqualByComparingTo("1.2500");
        // Liquidez corrente = 350k / 200k = 1,75
        assertThat(ex.liquidezCorrente()).isEqualByComparingTo("1.7500");
    }

    @Test
    void fleurietTipoII_solida() {
        Demonstrativo d = demonstrativo(2025);
        d.setCaixaBancos(new BigDecimal("80000"));          // AF = 80k
        d.setEmprestimosCurtoPrazo(new BigDecimal("20000")); // PF = 20k → T = +60k
        d.setContasReceber(new BigDecimal("150000"));
        d.setEstoques(new BigDecimal("50000"));              // AO = 200k
        d.setFornecedores(new BigDecimal("120000"));         // PO = 120k → NCG = +80k
        d.setImobilizado(new BigDecimal("300000"));          // ALP = 300k
        d.setPassivoNaoCirculante(new BigDecimal("140000"));
        d.setPatrimonioLiquido(new BigDecimal("300000"));    // PLP = 440k → CDG = +140k

        IndicadoresDTO.ExercicioDTO ex = calculator.calcular(List.of(d)).exercicios().get(0);

        assertThat(ex.tesouraria()).isEqualByComparingTo("60000");
        assertThat(ex.ncg()).isEqualByComparingTo("80000");
        assertThat(ex.cdg()).isEqualByComparingTo("140000");
        // Identidade de Fleuriet: T = CDG − NCG (o balanço fecha: dif = 0)
        assertThat(ex.diferencaBalanco()).isEqualByComparingTo("0");
        assertThat(ex.tipoFleuriet()).isEqualTo("Tipo II");
        assertThat(ex.diagnostico()).isEqualTo("Sólida");
    }

    @Test
    void fleurietTipoVI_critica() {
        Demonstrativo d = demonstrativo(2025);
        d.setEmprestimosCurtoPrazo(new BigDecimal("100000")); // T = −100k
        d.setFornecedores(new BigDecimal("50000"));
        d.setEstoques(new BigDecimal("30000"));               // NCG = 30k − 50k = −20k
        d.setImobilizado(new BigDecimal("200000"));
        d.setPatrimonioLiquido(new BigDecimal("80000"));      // CDG = 80k − 200k = −120k

        IndicadoresDTO.ExercicioDTO ex = calculator.calcular(List.of(d)).exercicios().get(0);

        assertThat(ex.tipoFleuriet()).isEqualTo("Tipo VI");
        assertThat(ex.diagnostico()).isEqualTo("Crítica");
    }

    @Test
    void evolucaoDeVendasEntreDoisExercicios() {
        Demonstrativo d1 = demonstrativo(2024);
        d1.setReceitaBruta(new BigDecimal("1000000"));
        Demonstrativo d2 = demonstrativo(2025);
        d2.setReceitaBruta(new BigDecimal("1100000"));

        IndicadoresDTO r = calculator.calcular(List.of(d2, d1)); // fora de ordem de propósito

        // (1.100.000 − 1.000.000) / 1.000.000 = 10%
        assertThat(r.evolucaoVendas()).isEqualByComparingTo("0.1000");
    }

    @Test
    void divisaoPorZeroViraNull_naoErro() {
        Demonstrativo d = demonstrativo(2025); // tudo zerado

        IndicadoresDTO r = calculator.calcular(List.of(d));
        IndicadoresDTO.ExercicioDTO ex = r.exercicios().get(0);

        assertThat(ex.roe()).isNull();            // PL = 0
        assertThat(ex.endividamento()).isNull();  // AT = 0
        assertThat(ex.liquidezSeca()).isNull();   // PC = 0
        assertThat(r.roeMedia()).isNull();
        assertThat(r.evolucaoVendas()).isNull();  // só 1 exercício
    }

    @Test
    void mediaDosIndicadoresEntreExercicios() {
        Demonstrativo d1 = demonstrativo(2024);
        d1.setLucroLiquido(new BigDecimal("10"));
        d1.setPatrimonioLiquido(new BigDecimal("100")); // ROE 0,10
        Demonstrativo d2 = demonstrativo(2025);
        d2.setLucroLiquido(new BigDecimal("30"));
        d2.setPatrimonioLiquido(new BigDecimal("100")); // ROE 0,30

        IndicadoresDTO r = calculator.calcular(List.of(d1, d2));

        assertThat(r.roeMedia()).isEqualByComparingTo("0.2000");
    }
}
