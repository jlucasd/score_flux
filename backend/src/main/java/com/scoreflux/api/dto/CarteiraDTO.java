package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Posição consolidada da carteira: uma linha por cliente. */
public record CarteiraDTO(
        List<PosicaoDTO> posicoes,
        BigDecimal totalLimite,
        BigDecimal totalSaldoAberto,
        BigDecimal totalDisponivel
) {

    public record PosicaoDTO(
            Long clienteId,
            String clienteNome,
            BigDecimal limite,          // da última análise concluída; null se não houver
            String rating,
            BigDecimal saldoAberto,
            BigDecimal disponivel,      // limite − saldoAberto; null se sem limite
            String status               // OK, BLOQUEAR ou SEM_LIMITE
    ) {
    }

    public record MovimentoDTO(
            Long id,
            LocalDate data,
            String tipo,
            BigDecimal valor,
            String descricao
    ) {
    }
}
