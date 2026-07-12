package com.scoreflux.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BalancoPdfParserTest {

    private final BalancoPdfParser parser = new BalancoPdfParser();

    @Test
    void extraiContasDeBalancoFormatoBrasileiro() {
        String texto = """
                BALANÇO PATRIMONIAL — Posição em 31/12/2025
                ATIVO
                Caixa e Bancos                                 150.000,00
                Aplicações Financeiras                          20.500,50
                Clientes                                       250.000,00
                Estoques                                        90.000,00
                Imobilizado                                    520.000,00
                PASSIVO
                Fornecedores                                   100.000,00
                Empréstimos e Financiamentos                    20.000,00
                Salários a Pagar                                 5.000,00
                Passivo Não Circulante                          50.000,00
                Patrimônio Líquido                             840.000,00
                DRE
                Receita Operacional Bruta                    1.200.000,00
                Lucro Líquido do Exercício                     200.000,00
                """;

        var r = parser.extrairDeTexto(texto);

        assertThat(r.exercicioDetectado()).isEqualTo(2025);
        assertThat(r.campos().get("caixaBancos")).isEqualByComparingTo("150000.00");
        assertThat(r.campos().get("aplicacoes")).isEqualByComparingTo("20500.50");
        assertThat(r.campos().get("contasReceber")).isEqualByComparingTo("250000.00");
        assertThat(r.campos().get("estoques")).isEqualByComparingTo("90000.00");
        assertThat(r.campos().get("imobilizado")).isEqualByComparingTo("520000.00");
        assertThat(r.campos().get("fornecedores")).isEqualByComparingTo("100000.00");
        assertThat(r.campos().get("emprestimosCurtoPrazo")).isEqualByComparingTo("20000.00");
        assertThat(r.campos().get("salariosAPagar")).isEqualByComparingTo("5000.00");
        assertThat(r.campos().get("passivoNaoCirculante")).isEqualByComparingTo("50000.00");
        assertThat(r.campos().get("patrimonioLiquido")).isEqualByComparingTo("840000.00");
        assertThat(r.campos().get("receitaBruta")).isEqualByComparingTo("1200000.00");
        assertThat(r.campos().get("lucroLiquido")).isEqualByComparingTo("200000.00");
    }

    @Test
    void valorEntreParentesesEhNegativo() {
        var r = parser.extrairDeTexto("Lucro/Prejuízo do período        (35.000,00)");

        assertThat(r.campos().get("lucroLiquido")).isEqualByComparingTo("-35000.00");
    }

    @Test
    void primeiraOcorrenciaVence() {
        String texto = """
                Fornecedores            80.000,00
                Fornecedores (LP)      999.999,00
                """;

        var r = parser.extrairDeTexto(texto);

        assertThat(r.campos().get("fornecedores")).isEqualByComparingTo("80000.00");
    }

    @Test
    void linhaSemNumeroNaoPreenche() {
        var r = parser.extrairDeTexto("Estoques\nOutra linha qualquer");

        assertThat(r.campos()).doesNotContainKey("estoques");
    }

    @Test
    void numeroSemVirgulaTrataPontoComoMilhar() {
        var r = parser.extrairDeTexto("Imobilizado      1.250.000");

        assertThat(r.campos().get("imobilizado")).isEqualByComparingTo("1250000");
    }
}
