package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Análise com respostas e resultado. Em RASCUNHO o resultado é calculado ao vivo;
 * em CONCLUIDA vem do snapshot congelado.
 */
public record AnaliseDetalheDTO(
        Long id,
        Long clienteId,
        String clienteNome,
        Long politicaId,
        String politicaNome,
        String status,
        String observacoes,
        LocalDateTime criadaEm,
        LocalDateTime concluidaEm,
        List<RespostaDTO> respostas,
        ResultadoDTO resultado,
        Map<Long, Long> sugestoes // subcriterioId -> opcaoId sugerida pelos demonstrativos
) {

    public record RespostaDTO(Long subcriterioId, Long opcaoId, String justificativa) {
    }

    public record ResultadoDTO(
            BigDecimal score,
            String rating,
            BigDecimal percentualLimite,
            BigDecimal baseLimite,
            BigDecimal limite
    ) {
    }
}
