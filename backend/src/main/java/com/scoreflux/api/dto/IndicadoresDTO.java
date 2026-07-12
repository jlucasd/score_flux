package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicadores e diagnóstico Fleuriet calculados a partir dos demonstrativos.
 * Campos null = não calculável (divisão por zero ou falta de dados).
 */
public record IndicadoresDTO(
        List<ExercicioDTO> exercicios,
        BigDecimal roeMedia,
        BigDecimal endividamentoMedia,
        BigDecimal liquidezSecaMedia,
        BigDecimal evolucaoVendas
) {

    public record ExercicioDTO(
            int exercicio,
            BigDecimal receitaBruta,
            BigDecimal lucroLiquido,
            BigDecimal patrimonioLiquido,
            BigDecimal ativoCirculante,
            BigDecimal passivoCirculante,
            BigDecimal ativoTotal,
            BigDecimal diferencaBalanco,
            BigDecimal roe,
            BigDecimal endividamento,
            BigDecimal liquidezSeca,
            BigDecimal liquidezCorrente,
            BigDecimal tesouraria,
            BigDecimal ncg,
            BigDecimal cdg,
            String tipoFleuriet,
            String diagnostico,
            String descricaoDiagnostico
    ) {
    }
}
