package com.scoreflux.service;

import com.scoreflux.api.dto.AnaliseDetalheDTO;
import com.scoreflux.api.dto.AnaliseDetalheDTO.RespostaDTO;
import com.scoreflux.api.dto.AnaliseDetalheDTO.ResultadoDTO;
import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.api.dto.PoliticaDTO;
import com.scoreflux.domain.*;
import com.scoreflux.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CreditoService {

    private final EmpresaRepository empresaRepository;
    private final ClienteRepository clienteRepository;
    private final DemonstrativoRepository demonstrativoRepository;
    private final PoliticaCreditoRepository politicaRepository;
    private final SubcriterioRepository subcriterioRepository;
    private final OpcaoRespostaRepository opcaoRepository;
    private final AnaliseCreditoRepository analiseRepository;
    private final RespostaAnaliseRepository respostaRepository;
    private final IndicadoresCalculator indicadoresCalculator;
    private final ScoreCalculator scoreCalculator;

    public CreditoService(EmpresaRepository empresaRepository,
                          ClienteRepository clienteRepository,
                          DemonstrativoRepository demonstrativoRepository,
                          PoliticaCreditoRepository politicaRepository,
                          SubcriterioRepository subcriterioRepository,
                          OpcaoRespostaRepository opcaoRepository,
                          AnaliseCreditoRepository analiseRepository,
                          RespostaAnaliseRepository respostaRepository,
                          IndicadoresCalculator indicadoresCalculator,
                          ScoreCalculator scoreCalculator) {
        this.empresaRepository = empresaRepository;
        this.clienteRepository = clienteRepository;
        this.demonstrativoRepository = demonstrativoRepository;
        this.politicaRepository = politicaRepository;
        this.subcriterioRepository = subcriterioRepository;
        this.opcaoRepository = opcaoRepository;
        this.analiseRepository = analiseRepository;
        this.respostaRepository = respostaRepository;
        this.indicadoresCalculator = indicadoresCalculator;
        this.scoreCalculator = scoreCalculator;
    }

    // ---- Clientes ----

    public List<Cliente> listarClientes() {
        return clienteRepository.findAllByOrderByNomeAsc();
    }

    public Cliente salvarCliente(Long id, Cliente dados) {
        Cliente cliente = id == null ? new Cliente()
                : clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + id));
        if (id == null) {
            Empresa empresa = empresaRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhuma empresa cadastrada"));
            cliente.setEmpresa(empresa);
        }
        cliente.setNome(dados.getNome());
        cliente.setCpfCnpj(dados.getCpfCnpj());
        cliente.setTipo(dados.getTipo() == null ? Cliente.Tipo.PRODUTOR : dados.getTipo());
        cliente.setMunicipio(dados.getMunicipio());
        cliente.setUf(dados.getUf());
        cliente.setTelefone(dados.getTelefone());
        cliente.setEmail(dados.getEmail());
        cliente.setEndereco(dados.getEndereco());
        return clienteRepository.save(cliente);
    }

    public void excluirCliente(Long id) {
        clienteRepository.deleteById(id);
    }

    // ---- Demonstrativos ----

    public List<Demonstrativo> listarDemonstrativos(Long clienteId) {
        return demonstrativoRepository.findByClienteIdOrderByExercicioAsc(clienteId);
    }

    public Demonstrativo salvarDemonstrativo(Long clienteId, int exercicio, Demonstrativo dados) {
        Cliente cliente = buscarCliente(clienteId);
        Demonstrativo alvo = demonstrativoRepository.findByClienteIdAndExercicio(clienteId, exercicio)
                .orElseGet(() -> {
                    Demonstrativo novo = new Demonstrativo();
                    novo.setCliente(cliente);
                    novo.setExercicio(exercicio);
                    return novo;
                });
        alvo.setReceitaBruta(nz(dados.getReceitaBruta()));
        alvo.setLucroLiquido(nz(dados.getLucroLiquido()));
        alvo.setCaixaBancos(nz(dados.getCaixaBancos()));
        alvo.setAplicacoes(nz(dados.getAplicacoes()));
        alvo.setContasReceber(nz(dados.getContasReceber()));
        alvo.setEstoques(nz(dados.getEstoques()));
        alvo.setOutrosAtivosCirculantes(nz(dados.getOutrosAtivosCirculantes()));
        alvo.setRealizavelLongoPrazo(nz(dados.getRealizavelLongoPrazo()));
        alvo.setImobilizado(nz(dados.getImobilizado()));
        alvo.setEmprestimosCurtoPrazo(nz(dados.getEmprestimosCurtoPrazo()));
        alvo.setFornecedores(nz(dados.getFornecedores()));
        alvo.setSalariosAPagar(nz(dados.getSalariosAPagar()));
        alvo.setOutrasObrigacoesCirculantes(nz(dados.getOutrasObrigacoesCirculantes()));
        alvo.setPassivoNaoCirculante(nz(dados.getPassivoNaoCirculante()));
        alvo.setPatrimonioLiquido(nz(dados.getPatrimonioLiquido()));
        return demonstrativoRepository.save(alvo);
    }

    public void excluirDemonstrativo(Long id) {
        demonstrativoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public IndicadoresDTO indicadores(Long clienteId) {
        buscarCliente(clienteId);
        return indicadoresCalculator.calcular(demonstrativoRepository.findByClienteIdOrderByExercicioAsc(clienteId));
    }

    // ---- Política ----

    @Transactional(readOnly = true)
    public PoliticaDTO politicaVigente() {
        PoliticaCredito politica = politicaRepository.findFirstByVigenteTrueOrderByVersaoDesc()
                .orElseThrow(() -> new IllegalStateException("Nenhuma política de crédito vigente"));
        List<Subcriterio> subcriterios = subcriterioRepository.findByPoliticaIdOrderByOrdemAsc(politica.getId());
        List<OpcaoResposta> opcoes = opcaoRepository
                .findBySubcriterioPoliticaIdOrderBySubcriterioOrdemAscOrdemAsc(politica.getId());

        Map<Long, List<PoliticaDTO.OpcaoDTO>> opcoesPorSub = new LinkedHashMap<>();
        for (OpcaoResposta o : opcoes) {
            opcoesPorSub.computeIfAbsent(o.getSubcriterio().getId(), k -> new ArrayList<>())
                    .add(new PoliticaDTO.OpcaoDTO(o.getId(), o.getRotulo(), o.getNota()));
        }

        List<PoliticaDTO.SubcriterioDTO> subs = subcriterios.stream()
                .map(s -> new PoliticaDTO.SubcriterioDTO(s.getId(), s.getGrupo(), s.getCodigo(), s.getNome(),
                        s.getPeso(), s.isAutomatico(), opcoesPorSub.getOrDefault(s.getId(), List.of())))
                .toList();

        return new PoliticaDTO(politica.getId(), politica.getVersao(), politica.getNome(),
                politica.getInflacaoReferencia(), subs);
    }

    // ---- Análises ----

    @Transactional(readOnly = true)
    public List<AnaliseCredito> listarAnalises(Long clienteId) {
        return analiseRepository.findByClienteIdOrderByCriadaEmDesc(clienteId);
    }

    public AnaliseCredito criarAnalise(Long clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        PoliticaCredito politica = politicaRepository.findFirstByVigenteTrueOrderByVersaoDesc()
                .orElseThrow(() -> new IllegalStateException("Nenhuma política de crédito vigente"));
        AnaliseCredito analise = new AnaliseCredito();
        analise.setCliente(cliente);
        analise.setPolitica(politica);
        return analiseRepository.save(analise);
    }

    public void salvarRespostas(Long analiseId, String observacoes, List<RespostaDTO> respostas) {
        AnaliseCredito analise = buscarAnalise(analiseId);
        exigirRascunho(analise);
        analise.setObservacoes(observacoes);

        for (RespostaDTO r : respostas) {
            Subcriterio sub = subcriterioRepository.findById(r.subcriterioId())
                    .orElseThrow(() -> new EntityNotFoundException("Subcritério não encontrado: " + r.subcriterioId()));
            OpcaoResposta opcao = opcaoRepository.findById(r.opcaoId())
                    .orElseThrow(() -> new EntityNotFoundException("Opção não encontrada: " + r.opcaoId()));
            if (!opcao.getSubcriterio().getId().equals(sub.getId())) {
                throw new IllegalArgumentException(
                        "Opção " + r.opcaoId() + " não pertence ao subcritério " + sub.getCodigo());
            }
            RespostaAnalise registro = respostaRepository
                    .findByAnaliseIdAndSubcriterioId(analiseId, sub.getId())
                    .orElseGet(() -> {
                        RespostaAnalise nova = new RespostaAnalise();
                        nova.setAnalise(analise);
                        nova.setSubcriterio(sub);
                        return nova;
                    });
            registro.setOpcao(opcao);
            registro.setJustificativa(r.justificativa());
            respostaRepository.save(registro);
        }
        analiseRepository.save(analise);
    }

    public AnaliseDetalheDTO concluir(Long analiseId) {
        AnaliseCredito analise = buscarAnalise(analiseId);
        exigirRascunho(analise);
        ScoreCalculator.Resultado resultado = calcularResultado(analise);
        analise.setScore(resultado.score());
        analise.setRating(resultado.rating());
        analise.setPercentualLimite(resultado.percentualLimite());
        analise.setBaseLimite(resultado.baseLimite());
        analise.setLimiteCalculado(resultado.limite());
        analise.setStatus(AnaliseCredito.Status.CONCLUIDA);
        analise.setConcluidaEm(LocalDateTime.now());
        analiseRepository.save(analise);
        return detalhe(analiseId);
    }

    public AnaliseDetalheDTO reabrir(Long analiseId) {
        AnaliseCredito analise = buscarAnalise(analiseId);
        analise.setStatus(AnaliseCredito.Status.RASCUNHO);
        analise.setConcluidaEm(null);
        analise.setScore(null);
        analise.setRating(null);
        analise.setPercentualLimite(null);
        analise.setBaseLimite(null);
        analise.setLimiteCalculado(null);
        analiseRepository.save(analise);
        return detalhe(analiseId);
    }

    public void excluirAnalise(Long analiseId) {
        analiseRepository.deleteById(analiseId);
    }

    @Transactional(readOnly = true)
    public AnaliseDetalheDTO detalhe(Long analiseId) {
        AnaliseCredito analise = buscarAnalise(analiseId);
        List<RespostaAnalise> respostas = respostaRepository.findByAnaliseId(analiseId);

        ResultadoDTO resultado;
        if (analise.getStatus() == AnaliseCredito.Status.CONCLUIDA) {
            resultado = new ResultadoDTO(analise.getScore(), analise.getRating(),
                    analise.getPercentualLimite(), analise.getBaseLimite(), analise.getLimiteCalculado());
        } else {
            ScoreCalculator.Resultado vivo = calcularResultado(analise);
            resultado = new ResultadoDTO(vivo.score(), vivo.rating(),
                    vivo.percentualLimite(), vivo.baseLimite(), vivo.limite());
        }

        return new AnaliseDetalheDTO(
                analise.getId(),
                analise.getCliente().getId(),
                analise.getCliente().getNome(),
                analise.getPolitica().getId(),
                analise.getPolitica().getNome(),
                analise.getStatus().name(),
                analise.getObservacoes(),
                analise.getCriadaEm(),
                analise.getConcluidaEm(),
                respostas.stream()
                        .map(r -> new RespostaDTO(r.getSubcriterio().getId(), r.getOpcao().getId(), r.getJustificativa()))
                        .toList(),
                resultado,
                sugestoes(analise));
    }

    /**
     * Sugestões automáticas para os subcritérios 3.x a partir dos demonstrativos,
     * aplicando as faixas anotadas na planilha (colunas I da aba Política Crédito).
     */
    private Map<Long, Long> sugestoes(AnaliseCredito analise) {
        List<Demonstrativo> demonstrativos =
                demonstrativoRepository.findByClienteIdOrderByExercicioAsc(analise.getCliente().getId());
        if (demonstrativos.isEmpty()) return Map.of();

        IndicadoresDTO ind = indicadoresCalculator.calcular(demonstrativos);
        BigDecimal inflacao = analise.getPolitica().getInflacaoReferencia()
                .divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP);

        Map<Long, Long> sugestoes = new LinkedHashMap<>();
        for (Subcriterio sub : subcriterioRepository.findByPoliticaIdOrderByOrdemAsc(analise.getPolitica().getId())) {
            if (!sub.isAutomatico()) continue;
            Integer nota = switch (sub.getCodigo()) {
                case "3.1" -> ind.evolucaoVendas() == null ? null
                        : ind.evolucaoVendas().compareTo(inflacao) > 0 ? 100
                        : ind.evolucaoVendas().signum() >= 0 ? 50 : 0;
                case "3.2" -> ind.roeMedia() == null ? null
                        : ind.roeMedia().compareTo(new BigDecimal("0.15")) >= 0 ? 100
                        : ind.roeMedia().compareTo(new BigDecimal("0.10")) >= 0 ? 50 : 0;
                case "3.3" -> ind.endividamentoMedia() == null ? null
                        : ind.endividamentoMedia().compareTo(new BigDecimal("0.50")) <= 0 ? 100
                        : ind.endividamentoMedia().compareTo(new BigDecimal("0.80")) < 0 ? 50 : 0;
                case "3.4" -> ind.liquidezSecaMedia() == null ? null
                        : ind.liquidezSecaMedia().compareTo(BigDecimal.ONE) > 0 ? 100 : 0;
                default -> null;
            };
            if (nota == null) continue;
            int alvo = nota;
            opcaoRepository.findBySubcriterioId(sub.getId()).stream()
                    .filter(o -> o.getNota() == alvo)
                    .findFirst()
                    .ifPresent(o -> sugestoes.put(sub.getId(), o.getId()));
        }
        return sugestoes;
    }

    private ScoreCalculator.Resultado calcularResultado(AnaliseCredito analise) {
        List<RespostaAnalise> respostas = respostaRepository.findByAnaliseId(analise.getId());
        List<Demonstrativo> demonstrativos =
                demonstrativoRepository.findByClienteIdOrderByExercicioAsc(analise.getCliente().getId());
        // Base do limite = PL e Faturamento do exercício mais recente (coluna C da planilha)
        BigDecimal pl = null;
        BigDecimal faturamento = null;
        if (!demonstrativos.isEmpty()) {
            Demonstrativo ultimo = demonstrativos.get(demonstrativos.size() - 1);
            pl = ultimo.getPatrimonioLiquido();
            faturamento = ultimo.getReceitaBruta();
        }
        return scoreCalculator.calcular(respostas, pl, faturamento);
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + id));
    }

    private AnaliseCredito buscarAnalise(Long id) {
        return analiseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Análise não encontrada: " + id));
    }

    private void exigirRascunho(AnaliseCredito analise) {
        if (analise.getStatus() != AnaliseCredito.Status.RASCUNHO) {
            throw new IllegalArgumentException("Análise concluída não pode ser alterada — reabra antes");
        }
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
