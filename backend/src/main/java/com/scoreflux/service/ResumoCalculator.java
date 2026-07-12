package com.scoreflux.service;

import com.scoreflux.api.dto.ResumoPlanoDTO;
import com.scoreflux.api.dto.ResumoPlanoDTO.ItemResumoDTO;
import com.scoreflux.api.dto.ResumoPlanoDTO.OrcamentoResumoDTO;
import com.scoreflux.domain.ItemFluxo;
import com.scoreflux.domain.Orcamento;
import com.scoreflux.domain.PlanoFluxo;
import com.scoreflux.domain.ValorMensal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Motor de cálculo do fluxo de pagamentos — equivalente às fórmulas da planilha:
 * total do item  = SUM(valores do item nos 12 meses)         [linha]
 * total do mês   = SUM(valores de todos os itens no mês)     [coluna]
 * total geral    = SUM(totais dos meses)
 * total orçamento = valor unitário × quantidade  /  total orçamentos = SUM
 * Classe pura, sem acesso a banco, para ser testável isoladamente.
 */
@Component
public class ResumoCalculator {

    public ResumoPlanoDTO calcular(PlanoFluxo plano,
                                   List<ItemFluxo> itens,
                                   List<ValorMensal> valores,
                                   List<Orcamento> orcamentos) {

        Map<Long, Map<Integer, BigDecimal>> valoresPorItem = valores.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getItem().getId(),
                        Collectors.toMap(ValorMensal::getMes, ValorMensal::getValor, BigDecimal::add)));

        Map<Integer, BigDecimal> totaisPorMes = new LinkedHashMap<>();
        for (int mes = 1; mes <= 12; mes++) {
            totaisPorMes.put(mes, BigDecimal.ZERO);
        }

        List<ItemResumoDTO> itensResumo = new ArrayList<>();
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (ItemFluxo item : itens) {
            Map<Integer, BigDecimal> valoresDoItem = valoresPorItem.getOrDefault(item.getId(), Map.of());
            Map<Integer, BigDecimal> porMes = new LinkedHashMap<>();
            BigDecimal totalItem = BigDecimal.ZERO;

            for (int mes = 1; mes <= 12; mes++) {
                BigDecimal valor = valoresDoItem.getOrDefault(mes, BigDecimal.ZERO);
                porMes.put(mes, valor);
                totalItem = totalItem.add(valor);
                totaisPorMes.merge(mes, valor, BigDecimal::add);
            }
            totalGeral = totalGeral.add(totalItem);

            itensResumo.add(new ItemResumoDTO(
                    item.getId(),
                    item.getDescricao(),
                    item.getMecanismo().name(),
                    item.getMecanismoOutro(),
                    item.getNota(),
                    item.getOrdem(),
                    porMes,
                    totalItem));
        }

        List<OrcamentoResumoDTO> orcamentosResumo = orcamentos.stream()
                .map(o -> new OrcamentoResumoDTO(
                        o.getId(),
                        o.getDescricao(),
                        o.getValorUnitario(),
                        o.getQuantidade(),
                        o.getValorUnitario().multiply(BigDecimal.valueOf(o.getQuantidade()))))
                .toList();

        BigDecimal totalOrcamentos = orcamentosResumo.stream()
                .map(OrcamentoResumoDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumoPlanoDTO(
                plano.getId(),
                plano.getNome(),
                plano.getAno(),
                itensResumo,
                totaisPorMes,
                totalGeral,
                orcamentosResumo,
                totalOrcamentos);
    }
}
