package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PoliticaDTO(
        Long id,
        int versao,
        String nome,
        BigDecimal inflacaoReferencia,
        List<SubcriterioDTO> subcriterios
) {

    public record SubcriterioDTO(
            Long id,
            String grupo,
            String codigo,
            String nome,
            BigDecimal peso,
            boolean automatico,
            List<OpcaoDTO> opcoes
    ) {
    }

    public record OpcaoDTO(Long id, String rotulo, int nota) {
    }
}
