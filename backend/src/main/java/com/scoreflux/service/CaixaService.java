package com.scoreflux.service;

import com.scoreflux.api.dto.ExtratoDTO;
import com.scoreflux.domain.Cliente;
import com.scoreflux.domain.Empresa;
import com.scoreflux.domain.LancamentoCaixa;
import com.scoreflux.repository.ClienteRepository;
import com.scoreflux.repository.EmpresaRepository;
import com.scoreflux.repository.LancamentoCaixaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CaixaService {

    private final LancamentoCaixaRepository lancamentoRepository;
    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final ExtratoCalculator extratoCalculator;

    public CaixaService(LancamentoCaixaRepository lancamentoRepository,
                        EmpresaRepository empresaRepository,
                        ClienteRepository clienteRepository,
                        ExtratoCalculator extratoCalculator) {
        this.lancamentoRepository = lancamentoRepository;
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.extratoCalculator = extratoCalculator;
    }

    public LancamentoCaixa criar(LocalDate data, String classificacao, String historico,
                                 BigDecimal entrada, BigDecimal saida,
                                 LancamentoCaixa.Status status, Long clienteId) {
        Empresa empresa = empresaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada"));
        LancamentoCaixa l = new LancamentoCaixa();
        l.setEmpresa(empresa);
        l.setData(data);
        l.setClassificacao(classificacao);
        l.setHistorico(historico);
        l.setEntrada(nz(entrada));
        l.setSaida(nz(saida));
        l.setStatus(status == null ? LancamentoCaixa.Status.REALIZADO : status);
        if (clienteId != null) {
            l.setCliente(clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId)));
        }
        return lancamentoRepository.save(l);
    }

    public void excluir(Long id) {
        lancamentoRepository.deleteById(id);
    }

    /** Extrato com saldo corrente. status null = todos (realizado + previsto). */
    @Transactional(readOnly = true)
    public ExtratoDTO extrato(LancamentoCaixa.Status status) {
        List<LancamentoCaixa> lancamentos = status == null
                ? lancamentoRepository.findAllByOrderByDataAscIdAsc()
                : lancamentoRepository.findByStatusOrderByDataAscIdAsc(status);
        return extratoCalculator.calcular(lancamentos, BigDecimal.ZERO);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
