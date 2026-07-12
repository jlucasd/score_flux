package com.scoreflux.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Espelho calculado da aba "Fluxo de pagamento": itens com valores mensais,
 * totais por mês (coluna), total por item (linha), total geral e orçamentos.
 * Nada disso é persistido — é sempre recalculado a partir dos lançamentos.
 */
public record ResumoPlanoDTO(
        Long planoId,
        String nome,
        int ano,
        List<ItemResumoDTO> itens,
        Map<Integer, BigDecimal> totaisPorMes,
        BigDecimal totalGeral,
        List<OrcamentoResumoDTO> orcamentos,
        BigDecimal totalOrcamentos
) {

    public record ItemResumoDTO(
            Long id,
            String descricao,
            String mecanismo,
            String mecanismoOutro,
            String nota,
            int ordem,
            Map<Integer, BigDecimal> valores,
            BigDecimal total
    ) {
    }

    public record OrcamentoResumoDTO(
            Long id,
            String descricao,
            BigDecimal valorUnitario,
            int quantidade,
            BigDecimal total
    ) {
    }
}
