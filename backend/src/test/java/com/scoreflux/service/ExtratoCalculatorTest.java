package com.scoreflux.service;

import com.scoreflux.api.dto.ExtratoDTO;
import com.scoreflux.domain.LancamentoCaixa;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratoCalculatorTest {

    private final ExtratoCalculator calculator = new ExtratoCalculator();

    private LancamentoCaixa lanc(String data, String entrada, String saida) {
        LancamentoCaixa l = new LancamentoCaixa();
        l.setData(LocalDate.parse(data));
        l.setEntrada(new BigDecimal(entrada));
        l.setSaida(new BigDecimal(saida));
        l.setStatus(LancamentoCaixa.Status.REALIZADO);
        return l;
    }

    @Test
    void saldoCorrenteAcumulaLinhaALinha() {
        // Fórmula da planilha: F(n) = F(n-1) + entrada − saída
        List<LancamentoCaixa> lancamentos = List.of(
                lanc("2026-03-01", "1000.00", "0"),
                lanc("2026-03-02", "0", "300.00"),
                lanc("2026-03-03", "500.00", "200.00"));

        ExtratoDTO extrato = calculator.calcular(lancamentos, BigDecimal.ZERO);

        assertThat(extrato.linhas()).hasSize(3);
        assertThat(extrato.linhas().get(0).saldo()).isEqualByComparingTo("1000.00");
        assertThat(extrato.linhas().get(1).saldo()).isEqualByComparingTo("700.00");
        assertThat(extrato.linhas().get(2).saldo()).isEqualByComparingTo("1000.00");
        assertThat(extrato.totalEntradas()).isEqualByComparingTo("1500.00");
        assertThat(extrato.totalSaidas()).isEqualByComparingTo("500.00");
        assertThat(extrato.saldoFinal()).isEqualByComparingTo("1000.00");
    }

    @Test
    void saldoInicialEhOPontoDePartida() {
        ExtratoDTO extrato = calculator.calcular(
                List.of(lanc("2026-03-01", "0", "100.00")), new BigDecimal("5000.00"));

        assertThat(extrato.linhas().get(0).saldo()).isEqualByComparingTo("4900.00");
        assertThat(extrato.saldoFinal()).isEqualByComparingTo("4900.00");
    }

    @Test
    void saldoPodeFicarNegativo() {
        ExtratoDTO extrato = calculator.calcular(
                List.of(lanc("2026-03-01", "0", "800.00")), BigDecimal.ZERO);

        assertThat(extrato.saldoFinal()).isEqualByComparingTo("-800.00");
    }

    @Test
    void extratoVazioSaldoIgualAoInicial() {
        ExtratoDTO extrato = calculator.calcular(List.of(), new BigDecimal("123.45"));

        assertThat(extrato.linhas()).isEmpty();
        assertThat(extrato.saldoFinal()).isEqualByComparingTo("123.45");
        assertThat(extrato.totalEntradas()).isEqualByComparingTo("0");
    }
}
