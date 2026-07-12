package com.scoreflux.service;

import com.scoreflux.api.dto.ExtratoDTO;
import com.scoreflux.api.dto.ExtratoDTO.LinhaDTO;
import com.scoreflux.domain.LancamentoCaixa;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor do saldo corrente das abas "Extrato Bancário" e "Contas Receber e Pagar":
 * saldo(n) = saldo(n-1) + entrada(n) − saída(n), partindo de um saldo inicial.
 * Classe pura, sem banco, para ser testada isoladamente.
 */
@Component
public class ExtratoCalculator {

    public ExtratoDTO calcular(List<LancamentoCaixa> lancamentos, BigDecimal saldoInicial) {
        BigDecimal saldo = saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
        BigDecimal totalEntradas = BigDecimal.ZERO;
        BigDecimal totalSaidas = BigDecimal.ZERO;

        List<LinhaDTO> linhas = new ArrayList<>();
        for (LancamentoCaixa l : lancamentos) {
            saldo = saldo.add(l.getEntrada()).subtract(l.getSaida());
            totalEntradas = totalEntradas.add(l.getEntrada());
            totalSaidas = totalSaidas.add(l.getSaida());
            linhas.add(new LinhaDTO(
                    l.getId(),
                    l.getData(),
                    l.getClassificacao(),
                    l.getHistorico(),
                    l.getEntrada(),
                    l.getSaida(),
                    saldo,
                    l.getStatus().name(),
                    l.getCliente() == null ? null : l.getCliente().getId(),
                    l.getCliente() == null ? null : l.getCliente().getNome()));
        }

        return new ExtratoDTO(linhas, totalEntradas, totalSaidas, saldo);
    }
}
