package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Extrato com saldo corrente calculado linha a linha (F(n) = F(n-1) + entrada − saída). */
public record ExtratoDTO(
        List<LinhaDTO> linhas,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal saldoFinal
) {

    public record LinhaDTO(
            Long id,
            LocalDate data,
            String classificacao,
            String historico,
            BigDecimal entrada,
            BigDecimal saida,
            BigDecimal saldo,
            String status,
            Long clienteId,
            String clienteNome
    ) {
    }
}
