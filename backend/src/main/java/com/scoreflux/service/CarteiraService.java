package com.scoreflux.service;

import com.scoreflux.api.dto.CarteiraDTO;
import com.scoreflux.api.dto.CarteiraDTO.MovimentoDTO;
import com.scoreflux.api.dto.CarteiraDTO.PosicaoDTO;
import com.scoreflux.domain.AnaliseCredito;
import com.scoreflux.domain.Cliente;
import com.scoreflux.domain.MovimentoCarteira;
import com.scoreflux.repository.AnaliseCreditoRepository;
import com.scoreflux.repository.ClienteRepository;
import com.scoreflux.repository.MovimentoCarteiraRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CarteiraService {

    private final ClienteRepository clienteRepository;
    private final MovimentoCarteiraRepository movimentoRepository;
    private final AnaliseCreditoRepository analiseRepository;

    public CarteiraService(ClienteRepository clienteRepository,
                           MovimentoCarteiraRepository movimentoRepository,
                           AnaliseCreditoRepository analiseRepository) {
        this.clienteRepository = clienteRepository;
        this.movimentoRepository = movimentoRepository;
        this.analiseRepository = analiseRepository;
    }

    @Transactional(readOnly = true)
    public CarteiraDTO carteira() {
        List<PosicaoDTO> posicoes = clienteRepository.findAllByOrderByNomeAsc().stream()
                .map(this::posicao)
                .toList();

        BigDecimal totalLimite = posicoes.stream()
                .map(p -> p.limite() == null ? BigDecimal.ZERO : p.limite())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaldo = posicoes.stream()
                .map(PosicaoDTO::saldoAberto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDisponivel = posicoes.stream()
                .map(p -> p.disponivel() == null ? BigDecimal.ZERO : p.disponivel())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarteiraDTO(posicoes, totalLimite, totalSaldo, totalDisponivel);
    }

    private PosicaoDTO posicao(Cliente cliente) {
        BigDecimal saldoAberto = saldoAberto(cliente.getId());

        // Limite vem da análise concluída mais recente que tenha limite calculado
        AnaliseCredito analise = analiseRepository.findByClienteIdOrderByCriadaEmDesc(cliente.getId()).stream()
                .filter(a -> a.getStatus() == AnaliseCredito.Status.CONCLUIDA && a.getLimiteCalculado() != null)
                .findFirst()
                .orElse(null);

        BigDecimal limite = analise == null ? null : analise.getLimiteCalculado();
        String rating = analise == null ? null : analise.getRating();
        BigDecimal disponivel = limite == null ? null : limite.subtract(saldoAberto);
        String status = limite == null ? "SEM_LIMITE" : (disponivel.signum() >= 0 ? "OK" : "BLOQUEAR");

        return new PosicaoDTO(cliente.getId(), cliente.getNome(), limite, rating, saldoAberto, disponivel, status);
    }

    private BigDecimal saldoAberto(Long clienteId) {
        return movimentoRepository.findByClienteIdOrderByDataAscIdAsc(clienteId).stream()
                .map(m -> m.getTipo() == MovimentoCarteira.Tipo.FATURAMENTO ? m.getValor() : m.getValor().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<MovimentoDTO> movimentos(Long clienteId) {
        return movimentoRepository.findByClienteIdOrderByDataAscIdAsc(clienteId).stream()
                .map(m -> new MovimentoDTO(m.getId(), m.getData(), m.getTipo().name(), m.getValor(), m.getDescricao()))
                .toList();
    }

    public void adicionarMovimento(Long clienteId, LocalDate data, MovimentoCarteira.Tipo tipo,
                                   BigDecimal valor, String descricao) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));
        MovimentoCarteira m = new MovimentoCarteira();
        m.setCliente(cliente);
        m.setData(data);
        m.setTipo(tipo);
        m.setValor(valor == null ? BigDecimal.ZERO : valor);
        m.setDescricao(descricao);
        movimentoRepository.save(m);
    }

    public void excluirMovimento(Long id) {
        movimentoRepository.deleteById(id);
    }
}
