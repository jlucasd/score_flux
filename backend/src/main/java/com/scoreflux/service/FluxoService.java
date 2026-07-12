package com.scoreflux.service;

import com.scoreflux.api.dto.ResumoPlanoDTO;
import com.scoreflux.domain.*;
import com.scoreflux.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FluxoService {

    private final EmpresaRepository empresaRepository;
    private final PlanoFluxoRepository planoRepository;
    private final ItemFluxoRepository itemRepository;
    private final ValorMensalRepository valorRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ResumoCalculator resumoCalculator;

    public FluxoService(EmpresaRepository empresaRepository,
                        PlanoFluxoRepository planoRepository,
                        ItemFluxoRepository itemRepository,
                        ValorMensalRepository valorRepository,
                        OrcamentoRepository orcamentoRepository,
                        ClienteRepository clienteRepository,
                        ResumoCalculator resumoCalculator) {
        this.empresaRepository = empresaRepository;
        this.planoRepository = planoRepository;
        this.itemRepository = itemRepository;
        this.valorRepository = valorRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.clienteRepository = clienteRepository;
        this.resumoCalculator = resumoCalculator;
    }

    // ---- Planos ----

    public List<PlanoFluxo> listarPlanos(String uf) {
        return (uf == null || uf.isBlank())
                ? planoRepository.findAllByOrderByAnoDescNomeAsc()
                : planoRepository.findByUfOrderByAnoDescNomeAsc(uf.toUpperCase());
    }

    public PlanoFluxo criarPlano(String nome, int ano, String uf, Long clienteId) {
        Empresa empresa = empresaRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada"));
        PlanoFluxo plano = new PlanoFluxo();
        plano.setEmpresa(empresa);
        plano.setNome(nome);
        plano.setAno(ano);

        Cliente cliente = null;
        if (clienteId != null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));
            plano.setCliente(cliente);
        }
        // UF explícita tem prioridade; se em branco, herda a do cliente vinculado
        String ufEfetiva = uf == null || uf.isBlank() ? (cliente == null ? null : cliente.getUf()) : uf;
        plano.setUf(ufEfetiva == null || ufEfetiva.isBlank() ? null : ufEfetiva.toUpperCase());
        return planoRepository.save(plano);
    }

    public void excluirPlano(Long planoId) {
        planoRepository.deleteById(planoId);
    }

    // ---- Itens ----

    public ItemFluxo criarItem(Long planoId, String descricao, Mecanismo mecanismo,
                               String mecanismoOutro, String nota) {
        PlanoFluxo plano = buscarPlano(planoId);
        ItemFluxo item = new ItemFluxo();
        item.setPlano(plano);
        preencherItem(item, descricao, mecanismo, mecanismoOutro, nota);
        item.setOrdem(itemRepository.findByPlanoIdOrderByOrdemAscIdAsc(planoId).size());
        return itemRepository.save(item);
    }

    public ItemFluxo atualizarItem(Long itemId, String descricao, Mecanismo mecanismo,
                                   String mecanismoOutro, String nota) {
        ItemFluxo item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado: " + itemId));
        preencherItem(item, descricao, mecanismo, mecanismoOutro, nota);
        return itemRepository.save(item);
    }

    public void excluirItem(Long itemId) {
        valorRepository.deleteAll(valorRepository.findByItemId(itemId));
        itemRepository.deleteById(itemId);
    }

    /**
     * Grava/atualiza os valores mensais do item (upsert por mês),
     * o equivalente a digitar nas células FEV..DEZ da planilha.
     */
    public void salvarValores(Long itemId, Map<Integer, BigDecimal> valoresPorMes) {
        ItemFluxo item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado: " + itemId));

        valoresPorMes.forEach((mes, valor) -> {
            if (mes < 1 || mes > 12) {
                throw new IllegalArgumentException("Mês inválido: " + mes);
            }
            ValorMensal registro = valorRepository.findByItemIdAndMes(itemId, mes)
                    .orElseGet(() -> {
                        ValorMensal novo = new ValorMensal();
                        novo.setItem(item);
                        novo.setMes(mes);
                        return novo;
                    });
            registro.setValor(valor == null ? BigDecimal.ZERO : valor);
            valorRepository.save(registro);
        });
    }

    // ---- Orçamentos ----

    public Orcamento criarOrcamento(Long planoId, String descricao, BigDecimal valorUnitario, int quantidade) {
        PlanoFluxo plano = buscarPlano(planoId);
        Orcamento orcamento = new Orcamento();
        orcamento.setPlano(plano);
        orcamento.setDescricao(descricao);
        orcamento.setValorUnitario(valorUnitario);
        orcamento.setQuantidade(quantidade);
        return orcamentoRepository.save(orcamento);
    }

    public void excluirOrcamento(Long orcamentoId) {
        orcamentoRepository.deleteById(orcamentoId);
    }

    // ---- Resumo (as "fórmulas" da planilha) ----

    @Transactional(readOnly = true)
    public ResumoPlanoDTO resumo(Long planoId) {
        PlanoFluxo plano = buscarPlano(planoId);
        return resumoCalculator.calcular(
                plano,
                itemRepository.findByPlanoIdOrderByOrdemAscIdAsc(planoId),
                valorRepository.findByItemPlanoId(planoId),
                orcamentoRepository.findByPlanoIdOrderByIdAsc(planoId));
    }

    private PlanoFluxo buscarPlano(Long planoId) {
        return planoRepository.findById(planoId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado: " + planoId));
    }

    private void preencherItem(ItemFluxo item, String descricao, Mecanismo mecanismo,
                               String mecanismoOutro, String nota) {
        item.setDescricao(descricao);
        item.setMecanismo(mecanismo);
        item.setMecanismoOutro(mecanismo == Mecanismo.OUTRO ? mecanismoOutro : null);
        item.setNota(nota);
    }
}
