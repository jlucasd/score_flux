package com.scoreflux.service;

import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.api.dto.IndicadoresDTO.ExercicioDTO;
import com.scoreflux.domain.Demonstrativo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Indicadores da aba "Análise Indicadores" + modelo Fleuriet da aba "NCG":
 * ROE            = Lucro Líquido / PL
 * Endividamento  = (PC + PNC) / Ativo Total
 * Liquidez Seca  = (AC − Estoques) / PC
 * Liquidez Corr. = AC / PC
 * Evolução Vendas = RB(ano2) / RB(ano1) − 1
 * Tesouraria (T) = Ativo Financeiro − Passivo Financeiro
 * NCG            = Ativo Operacional − Passivo Operacional
 * CDG            = (PNC + PL) − (RLP + Imobilizado)
 * Divisão por zero → indicador null (equivalente ao #DIV/0! da planilha).
 */
@Component
public class IndicadoresCalculator {

    private static final int ESCALA = 4;

    public IndicadoresDTO calcular(List<Demonstrativo> demonstrativos) {
        List<Demonstrativo> ordenados = demonstrativos.stream()
                .sorted(Comparator.comparingInt(Demonstrativo::getExercicio))
                .toList();

        List<ExercicioDTO> exercicios = new ArrayList<>();
        for (Demonstrativo d : ordenados) {
            exercicios.add(calcularExercicio(d));
        }

        BigDecimal evolucaoVendas = null;
        if (ordenados.size() >= 2) {
            Demonstrativo anterior = ordenados.get(ordenados.size() - 2);
            Demonstrativo atual = ordenados.get(ordenados.size() - 1);
            BigDecimal variacao = dividir(atual.getReceitaBruta().subtract(anterior.getReceitaBruta()),
                    anterior.getReceitaBruta());
            evolucaoVendas = variacao;
        }

        return new IndicadoresDTO(
                exercicios,
                media(exercicios.stream().map(ExercicioDTO::roe).toList()),
                media(exercicios.stream().map(ExercicioDTO::endividamento).toList()),
                media(exercicios.stream().map(ExercicioDTO::liquidezSeca).toList()),
                evolucaoVendas);
    }

    private ExercicioDTO calcularExercicio(Demonstrativo d) {
        BigDecimal ac = d.getAtivoCirculante();
        BigDecimal pc = d.getPassivoCirculante();
        BigDecimal ativoTotal = d.getAtivoTotal();

        BigDecimal roe = dividir(d.getLucroLiquido(), d.getPatrimonioLiquido());
        BigDecimal endividamento = dividir(pc.add(d.getPassivoNaoCirculante()), ativoTotal);
        BigDecimal liquidezSeca = dividir(ac.subtract(d.getEstoques()), pc);
        BigDecimal liquidezCorrente = dividir(ac, pc);

        // Fleuriet: reclassificação financeiro × operacional × longo prazo
        BigDecimal ativoFinanceiro = d.getCaixaBancos().add(d.getAplicacoes());
        BigDecimal passivoFinanceiro = d.getEmprestimosCurtoPrazo();
        BigDecimal ativoOperacional = d.getContasReceber().add(d.getEstoques()).add(d.getOutrosAtivosCirculantes());
        BigDecimal passivoOperacional = d.getFornecedores().add(d.getSalariosAPagar()).add(d.getOutrasObrigacoesCirculantes());
        BigDecimal aplicacoesLongoPrazo = d.getRealizavelLongoPrazo().add(d.getImobilizado());
        BigDecimal fontesLongoPrazo = d.getPassivoNaoCirculante().add(d.getPatrimonioLiquido());

        BigDecimal tesouraria = ativoFinanceiro.subtract(passivoFinanceiro);
        BigDecimal ncg = ativoOperacional.subtract(passivoOperacional);
        BigDecimal cdg = fontesLongoPrazo.subtract(aplicacoesLongoPrazo);

        BigDecimal passivoTotal = pc.add(d.getPassivoNaoCirculante()).add(d.getPatrimonioLiquido());
        BigDecimal diferencaBalanco = ativoTotal.subtract(passivoTotal);

        TipoFleuriet tipo = classificar(cdg, ncg, tesouraria);

        return new ExercicioDTO(
                d.getExercicio(), d.getReceitaBruta(), d.getLucroLiquido(), d.getPatrimonioLiquido(),
                ac, pc, ativoTotal, diferencaBalanco,
                roe, endividamento, liquidezSeca, liquidezCorrente,
                tesouraria, ncg, cdg,
                tipo.nome, tipo.diagnostico, tipo.descricao);
    }

    /** Tabela "Possíveis Resultados" da aba NCG (sinais de CDG, NCG e T). */
    private TipoFleuriet classificar(BigDecimal cdg, BigDecimal ncg, BigDecimal t) {
        boolean cdgPos = cdg.signum() >= 0;
        boolean ncgPos = ncg.signum() >= 0;
        boolean tPos = t.signum() >= 0;

        if (cdgPos && !ncgPos && tPos) return new TipoFleuriet("Tipo I", "Excelente", "Operação gera caixa e estrutura financeira sólida");
        if (cdgPos && ncgPos && tPos) return new TipoFleuriet("Tipo II", "Sólida", "CDG cobre a NCG com folga");
        if (cdgPos && ncgPos) return new TipoFleuriet("Tipo III", "Atenção", "Dependência de financiamento de curto prazo");
        if (!cdgPos && ncgPos) return new TipoFleuriet("Tipo IV", "Insatisfatória", "Operação consome caixa e estrutura inadequada");
        if (!cdgPos && tPos) return new TipoFleuriet("Tipo V", "Estrutura Fraca", "Caixa positivo, mas longo prazo financiado no curto");
        if (!cdgPos) return new TipoFleuriet("Tipo VI", "Crítica", "Desequilíbrio grave e risco de insolvência");
        return new TipoFleuriet("—", "Indeterminado", "Combinação atípica — verifique se o balanço fecha");
    }

    private record TipoFleuriet(String nome, String diagnostico, String descricao) {
    }

    private BigDecimal dividir(BigDecimal numerador, BigDecimal denominador) {
        if (denominador == null || denominador.signum() == 0) return null;
        return numerador.divide(denominador, ESCALA, RoundingMode.HALF_UP);
    }

    private BigDecimal media(List<BigDecimal> valores) {
        List<BigDecimal> presentes = valores.stream().filter(v -> v != null).toList();
        if (presentes.isEmpty()) return null;
        BigDecimal soma = presentes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(presentes.size()), ESCALA, RoundingMode.HALF_UP);
    }
}
