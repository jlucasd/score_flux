package com.scoreflux.service;

import com.scoreflux.api.dto.ResumoPlanoDTO;
import com.scoreflux.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoCalculatorTest {

    private final ResumoCalculator calculator = new ResumoCalculator();

    private PlanoFluxo plano() {
        PlanoFluxo plano = new PlanoFluxo();
        plano.setId(1L);
        plano.setNome("Fluxo 2026");
        plano.setAno(2026);
        return plano;
    }

    private ItemFluxo item(long id, String descricao) {
        ItemFluxo item = new ItemFluxo();
        item.setId(id);
        item.setDescricao(descricao);
        item.setMecanismo(Mecanismo.BOLETO);
        return item;
    }

    private ValorMensal valor(ItemFluxo item, int mes, String valor) {
        ValorMensal v = new ValorMensal();
        v.setItem(item);
        v.setMes(mes);
        v.setValor(new BigDecimal(valor));
        return v;
    }

    @Test
    void totalDoItemEhASomaDosMeses() {
        // Fórmula da planilha: total da linha = SUM(FEV..DEZ)
        ItemFluxo trator = item(10L, "01 Trator");
        List<ValorMensal> valores = List.of(
                valor(trator, 2, "1000.00"),
                valor(trator, 3, "1000.00"),
                valor(trator, 4, "500.50"));

        ResumoPlanoDTO resumo = calculator.calcular(plano(), List.of(trator), valores, List.of());

        assertThat(resumo.itens()).hasSize(1);
        assertThat(resumo.itens().get(0).total()).isEqualByComparingTo("2500.50");
        assertThat(resumo.itens().get(0).valores()).containsEntry(2, new BigDecimal("1000.00"));
        assertThat(resumo.itens().get(0).valores().get(1)).isEqualByComparingTo("0"); // mês sem lançamento
    }

    @Test
    void totalDoMesEhASomaDosItensNaColuna() {
        ItemFluxo trator = item(10L, "01 Trator");
        ItemFluxo fretes = item(20L, "Fretes");
        List<ValorMensal> valores = List.of(
                valor(trator, 5, "300.00"),
                valor(fretes, 5, "200.00"),
                valor(fretes, 6, "150.00"));

        ResumoPlanoDTO resumo = calculator.calcular(plano(), List.of(trator, fretes), valores, List.of());

        assertThat(resumo.totaisPorMes().get(5)).isEqualByComparingTo("500.00");
        assertThat(resumo.totaisPorMes().get(6)).isEqualByComparingTo("150.00");
        assertThat(resumo.totaisPorMes().get(1)).isEqualByComparingTo("0");
        assertThat(resumo.totaisPorMes()).hasSize(12); // sempre os 12 meses presentes
    }

    @Test
    void totalGeralEhASomaDeTudo() {
        ItemFluxo a = item(1L, "A");
        ItemFluxo b = item(2L, "B");
        List<ValorMensal> valores = List.of(
                valor(a, 1, "100.00"),
                valor(a, 12, "200.00"),
                valor(b, 6, "50.00"));

        ResumoPlanoDTO resumo = calculator.calcular(plano(), List.of(a, b), valores, List.of());

        assertThat(resumo.totalGeral()).isEqualByComparingTo("350.00");
    }

    @Test
    void planoVazioRetornaZerosEmTodosOsMeses() {
        ResumoPlanoDTO resumo = calculator.calcular(plano(), List.of(), List.of(), List.of());

        assertThat(resumo.itens()).isEmpty();
        assertThat(resumo.totalGeral()).isEqualByComparingTo("0");
        assertThat(resumo.totaisPorMes()).hasSize(12);
        resumo.totaisPorMes().values().forEach(v -> assertThat(v).isEqualByComparingTo("0"));
    }

    @Test
    void orcamentoMultiplicaValorUnitarioPelaQuantidade() {
        // Fórmulas da planilha: E28 = C28*D28 e total = SUM(E28:E30)
        Orcamento escavadeiras = new Orcamento();
        escavadeiras.setId(1L);
        escavadeiras.setDescricao("Escavadeiras");
        escavadeiras.setValorUnitario(new BigDecimal("450000.00"));
        escavadeiras.setQuantidade(5);

        Orcamento pa = new Orcamento();
        pa.setId(2L);
        pa.setDescricao("Pá-Carregadeira");
        pa.setValorUnitario(new BigDecimal("380000.00"));
        pa.setQuantidade(10);

        ResumoPlanoDTO resumo = calculator.calcular(plano(), List.of(), List.of(), List.of(escavadeiras, pa));

        assertThat(resumo.orcamentos().get(0).total()).isEqualByComparingTo("2250000.00");
        assertThat(resumo.orcamentos().get(1).total()).isEqualByComparingTo("3800000.00");
        assertThat(resumo.totalOrcamentos()).isEqualByComparingTo("6050000.00");
    }
}
