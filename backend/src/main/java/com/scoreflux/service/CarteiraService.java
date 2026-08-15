package com.scoreflux.service;

import com.scoreflux.api.dto.CarteiraDTO;
import com.scoreflux.api.dto.CarteiraDTO.MovimentoDTO;
import com.scoreflux.api.dto.CarteiraDTO.PosicaoDTO;
import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.domain.*;
import com.scoreflux.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CarteiraService {

    private final ClienteRepository clienteRepository;
    private final MovimentoCarteiraRepository movimentoRepository;
    private final AnaliseCreditoRepository analiseRepository;
    private final RelatoCampoRepository relatoRepository;
    private final DemonstrativoRepository demonstrativoRepository;
    private final SubcriterioRepository subcriterioRepository;
    private final OpcaoRespostaRepository opcaoRepository;
    private final PoliticaCreditoRepository politicaRepository;
    private final IndicadoresCalculator indicadoresCalculator;

    public CarteiraService(ClienteRepository clienteRepository,
                           MovimentoCarteiraRepository movimentoRepository,
                           AnaliseCreditoRepository analiseRepository,
                           RelatoCampoRepository relatoRepository,
                           DemonstrativoRepository demonstrativoRepository,
                           SubcriterioRepository subcriterioRepository,
                           OpcaoRespostaRepository opcaoRepository,
                           PoliticaCreditoRepository politicaRepository,
                           IndicadoresCalculator indicadoresCalculator) {
        this.clienteRepository = clienteRepository;
        this.movimentoRepository = movimentoRepository;
        this.analiseRepository = analiseRepository;
        this.relatoRepository = relatoRepository;
        this.demonstrativoRepository = demonstrativoRepository;
        this.subcriterioRepository = subcriterioRepository;
        this.opcaoRepository = opcaoRepository;
        this.politicaRepository = politicaRepository;
        this.indicadoresCalculator = indicadoresCalculator;
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

        AnaliseCredito analise = analiseRepository.findByClienteIdOrderByCriadaEmDesc(cliente.getId()).stream()
                .filter(a -> a.getStatus() == AnaliseCredito.Status.CONCLUIDA && a.getLimiteCalculado() != null)
                .findFirst()
                .orElse(null);

        BigDecimal limite;
        String rating;

        if (analise != null) {
            limite = analise.getLimiteCalculado();
            rating = analise.getRating();
        } else {
            var simulado = simularPolitica(cliente.getId());
            limite = simulado.limite;
            rating = simulado.rating;
        }

        BigDecimal disponivel = limite == null ? null : limite.subtract(saldoAberto);
        String status = limite == null ? "SEM_LIMITE" : (disponivel.signum() >= 0 ? "OK" : "BLOQUEAR");

        return new PosicaoDTO(cliente.getId(), cliente.getNome(), limite, rating, saldoAberto, disponivel, status);
    }

    private record Simulado(String rating, BigDecimal limite) {}

    private Simulado simularPolitica(Long clienteId) {
        Optional<PoliticaCredito> polOpt = politicaRepository.findFirstByVigenteTrueOrderByVersaoDesc();
        if (polOpt.isEmpty()) return new Simulado(null, null);

        PoliticaCredito pol = polOpt.get();
        List<Subcriterio> subcriterios = subcriterioRepository.findByPoliticaIdOrderByOrdemAsc(pol.getId());

        RelatoCampo relato = relatoRepository.findFirstByClienteIdOrderByAtualizadoEmDesc(clienteId).orElse(null);
        List<Demonstrativo> demos = demonstrativoRepository.findByClienteIdOrderByExercicioAsc(clienteId);
        IndicadoresDTO indicadores = demos.isEmpty() ? null : indicadoresCalculator.calcular(demos);

        BigDecimal score = BigDecimal.ZERO;
        boolean temAlgum = false;

        for (Subcriterio sub : subcriterios) {
            List<OpcaoResposta> opcoes = opcaoRepository.findBySubcriterioId(sub.getId());
            OpcaoResposta match = matchOpcao(sub, opcoes, relato, indicadores,
                    pol.getInflacaoReferencia().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            if (match != null) {
                score = score.add(sub.getPeso().multiply(BigDecimal.valueOf(match.getNota())));
                temAlgum = true;
            }
        }

        if (!temAlgum) return new Simulado(null, null);

        BigDecimal scoreFinal = score.setScale(2, RoundingMode.HALF_UP);
        ScoreCalculator.Faixa faixa = ScoreCalculator.FAIXAS.stream()
                .filter(f -> scoreFinal.compareTo(f.scoreMinimo()) >= 0)
                .findFirst()
                .orElse(ScoreCalculator.FAIXAS.get(ScoreCalculator.FAIXAS.size() - 1));

        BigDecimal limite = null;
        if (!demos.isEmpty()) {
            Demonstrativo ultimo = demos.get(demos.size() - 1);
            BigDecimal base = ultimo.getPatrimonioLiquido().add(ultimo.getReceitaBruta())
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            limite = base.multiply(faixa.percentualLimite()).setScale(2, RoundingMode.HALF_UP);
        }

        return new Simulado(faixa.rating(), limite);
    }

    private OpcaoResposta matchOpcao(Subcriterio sub, List<OpcaoResposta> opcoes,
                                     RelatoCampo relato, IndicadoresDTO indicadores, BigDecimal inflacao) {
        if (relato != null) {
            String valor = switch (sub.getCodigo()) {
                case "1.1" -> relato.getConceitoComercial();
                case "2.3" -> relato.getTempoMercado();
                case "4.1" -> relato.getBandeira();
                case "4.3" -> relato.getUnidadesNegocio();
                case "4.4" -> relato.getRiscoClimatico();
                case "4.2" -> {
                    Boolean erp = relato.getPossuiErp();
                    Boolean cob = relato.getPossuiCobranca();
                    if (erp == null && cob == null) yield null;
                    if (Boolean.TRUE.equals(erp) && Boolean.TRUE.equals(cob))
                        yield opcoes.isEmpty() ? null : opcoes.get(0).getRotulo();
                    else if (Boolean.TRUE.equals(erp) || Boolean.TRUE.equals(cob))
                        yield opcoes.size() > 1 ? opcoes.get(1).getRotulo() : null;
                    else
                        yield opcoes.isEmpty() ? null : opcoes.get(opcoes.size() - 1).getRotulo();
                }
                default -> null;
            };
            if (valor != null) {
                return opcoes.stream().filter(o -> o.getRotulo().equals(valor)).findFirst().orElse(null);
            }
        }

        if (indicadores != null && sub.isAutomatico()) {
            Integer nota = switch (sub.getCodigo()) {
                case "3.1" -> indicadores.evolucaoVendas() == null ? null
                        : indicadores.evolucaoVendas().compareTo(inflacao) > 0 ? 100
                        : indicadores.evolucaoVendas().signum() >= 0 ? 50 : 0;
                case "3.2" -> indicadores.roeMedia() == null ? null
                        : indicadores.roeMedia().compareTo(new BigDecimal("0.15")) >= 0 ? 100
                        : indicadores.roeMedia().compareTo(new BigDecimal("0.10")) >= 0 ? 50 : 0;
                case "3.3" -> indicadores.endividamentoMedia() == null ? null
                        : indicadores.endividamentoMedia().compareTo(new BigDecimal("0.50")) <= 0 ? 100
                        : indicadores.endividamentoMedia().compareTo(new BigDecimal("0.80")) < 0 ? 50 : 0;
                case "3.4" -> indicadores.liquidezSecaMedia() == null ? null
                        : indicadores.liquidezSecaMedia().compareTo(BigDecimal.ONE) > 0 ? 100 : 0;
                default -> null;
            };
            if (nota != null) {
                int alvo = nota;
                return opcoes.stream().filter(o -> o.getNota() == alvo).findFirst().orElse(null);
            }
        }

        return null;
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
