package com.scoreflux.service;

import com.scoreflux.domain.RespostaAnalise;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Motor de score da aba "Política Crédito":
 * score = Σ (peso_subcritério × nota_opção)  → escala 0–100 (pesos somam 1)
 * rating pela tabela de 16 faixas; limite = média(PL, Faturamento) × % do rating.
 * Subcritério sem resposta pontua 0 (mesmo comportamento da planilha, H=0).
 */
@Component
public class ScoreCalculator {

    public record Faixa(BigDecimal scoreMinimo, String rating, BigDecimal percentualLimite) {
    }

    // Tabela D93:G107 da planilha (limite inferior de cada faixa, em ordem decrescente)
    public static final List<Faixa> FAIXAS = List.of(
            new Faixa(new BigDecimal("94.6"), "AAA", new BigDecimal("0.15")),
            new Faixa(new BigDecimal("89.2"), "AA", new BigDecimal("0.14")),
            new Faixa(new BigDecimal("83.8"), "A+", new BigDecimal("0.13")),
            new Faixa(new BigDecimal("78.4"), "A-", new BigDecimal("0.12")),
            new Faixa(new BigDecimal("73"), "B+", new BigDecimal("0.11")),
            new Faixa(new BigDecimal("67.6"), "B", new BigDecimal("0.10")),
            new Faixa(new BigDecimal("62.2"), "B-", new BigDecimal("0.09")),
            new Faixa(new BigDecimal("56.8"), "C+", new BigDecimal("0.08")),
            new Faixa(new BigDecimal("51.4"), "C", new BigDecimal("0.07")),
            new Faixa(new BigDecimal("46"), "C-", new BigDecimal("0.06")),
            new Faixa(new BigDecimal("36.8"), "D", BigDecimal.ZERO),
            new Faixa(new BigDecimal("27.6"), "E", BigDecimal.ZERO),
            new Faixa(new BigDecimal("18.4"), "F", BigDecimal.ZERO),
            new Faixa(new BigDecimal("9.2"), "G", BigDecimal.ZERO),
            new Faixa(BigDecimal.ZERO, "H", BigDecimal.ZERO));

    public record Resultado(BigDecimal score, String rating, BigDecimal percentualLimite,
                            BigDecimal baseLimite, BigDecimal limite) {
    }

    public Resultado calcular(List<RespostaAnalise> respostas,
                              BigDecimal patrimonioLiquido,
                              BigDecimal faturamento) {
        BigDecimal score = respostas.stream()
                .map(r -> r.getSubcriterio().getPeso().multiply(BigDecimal.valueOf(r.getOpcao().getNota())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Faixa faixa = FAIXAS.stream()
                .filter(f -> score.compareTo(f.scoreMinimo()) >= 0)
                .findFirst()
                .orElse(FAIXAS.get(FAIXAS.size() - 1));

        BigDecimal baseLimite = null;
        BigDecimal limite = null;
        if (patrimonioLiquido != null && faturamento != null) {
            baseLimite = patrimonioLiquido.add(faturamento)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            limite = baseLimite.multiply(faixa.percentualLimite()).setScale(2, RoundingMode.HALF_UP);
        }

        return new Resultado(score, faixa.rating(), faixa.percentualLimite(), baseLimite, limite);
    }
}
