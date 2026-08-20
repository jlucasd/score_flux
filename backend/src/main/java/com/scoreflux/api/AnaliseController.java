package com.scoreflux.api;

import com.scoreflux.api.dto.AnaliseDetalheDTO;
import com.scoreflux.api.dto.PoliticaDTO;
import com.scoreflux.domain.*;
import com.scoreflux.repository.ClienteRepository;
import com.scoreflux.repository.OpcaoRespostaRepository;
import com.scoreflux.repository.PesoAtribuidoRepository;
import com.scoreflux.repository.SubcriterioRepository;
import com.scoreflux.service.CreditoService;
import com.scoreflux.service.ParecerPdfService;
import com.scoreflux.service.ScoreCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AnaliseController {

    private final CreditoService service;
    private final ParecerPdfService parecerPdfService;
    private final SubcriterioRepository subcriterioRepository;
    private final PesoAtribuidoRepository pesoAtribuidoRepository;
    private final ClienteRepository clienteRepository;
    private final OpcaoRespostaRepository opcaoRespostaRepository;

    public AnaliseController(CreditoService service, ParecerPdfService parecerPdfService,
                             SubcriterioRepository subcriterioRepository,
                             PesoAtribuidoRepository pesoAtribuidoRepository,
                             ClienteRepository clienteRepository,
                             OpcaoRespostaRepository opcaoRespostaRepository) {
        this.service = service;
        this.parecerPdfService = parecerPdfService;
        this.subcriterioRepository = subcriterioRepository;
        this.pesoAtribuidoRepository = pesoAtribuidoRepository;
        this.clienteRepository = clienteRepository;
        this.opcaoRespostaRepository = opcaoRespostaRepository;
    }

    public record AnaliseResumoResponse(Long id, String status, LocalDateTime criadaEm,
                                        LocalDateTime concluidaEm, BigDecimal score, String rating,
                                        BigDecimal limiteCalculado) {
        static AnaliseResumoResponse de(AnaliseCredito a) {
            return new AnaliseResumoResponse(a.getId(), a.getStatus().name(), a.getCriadaEm(),
                    a.getConcluidaEm(), a.getScore(), a.getRating(), a.getLimiteCalculado());
        }
    }

    public record SalvarRespostasRequest(String observacoes,
                                         List<AnaliseDetalheDTO.RespostaDTO> respostas) {
    }

    public record FaixaResponse(BigDecimal scoreMinimo, String rating, BigDecimal percentualLimite) {
        static FaixaResponse de(ScoreCalculator.Faixa f) {
            return new FaixaResponse(f.scoreMinimo(), f.rating(), f.percentualLimite());
        }
    }

    @GetMapping("/politica")
    public PoliticaDTO politica() {
        return service.politicaVigente();
    }

    @GetMapping("/politica/faixas")
    public List<FaixaResponse> faixas() {
        return ScoreCalculator.FAIXAS.stream().map(FaixaResponse::de).toList();
    }

    @GetMapping("/clientes/{clienteId}/analises")
    public List<AnaliseResumoResponse> listar(@PathVariable Long clienteId) {
        return service.listarAnalises(clienteId).stream().map(AnaliseResumoResponse::de).toList();
    }

    @PostMapping("/clientes/{clienteId}/analises")
    @ResponseStatus(HttpStatus.CREATED)
    public AnaliseDetalheDTO criar(@PathVariable Long clienteId) {
        return service.detalhe(service.criarAnalise(clienteId).getId());
    }

    @GetMapping("/analises/{id}")
    public AnaliseDetalheDTO detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }

    @PutMapping("/analises/{id}/respostas")
    public AnaliseDetalheDTO salvarRespostas(@PathVariable Long id,
                                             @RequestBody SalvarRespostasRequest r) {
        service.salvarRespostas(id, r.observacoes(), r.respostas() == null ? List.of() : r.respostas());
        return service.detalhe(id);
    }

    @PostMapping("/analises/{id}/concluir")
    public AnaliseDetalheDTO concluir(@PathVariable Long id) {
        return service.concluir(id);
    }

    @PostMapping("/analises/{id}/reabrir")
    public AnaliseDetalheDTO reabrir(@PathVariable Long id) {
        return service.reabrir(id);
    }

    @DeleteMapping("/analises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirAnalise(id);
    }

    public record PesoRequest(BigDecimal peso) {}

    @PutMapping("/subcriterios/{id}/peso")
    public PoliticaDTO atualizarPeso(@PathVariable Long id, @RequestBody PesoRequest r) {
        Subcriterio sub = subcriterioRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Subcritério não encontrado: " + id));
        sub.setPeso(r.peso());
        subcriterioRepository.save(sub);
        return service.politicaVigente();
    }

    // ---- Peso Atribuído (Política de Crédito) ----

    public record PesoAtribuidoItem(Long opcaoId, int valor) {}

    @GetMapping("/clientes/{clienteId}/pesos-atribuidos")
    public Map<Long, Integer> pesosAtribuidos(@PathVariable Long clienteId) {
        return pesoAtribuidoRepository.findByClienteId(clienteId).stream()
                .collect(Collectors.toMap(p -> p.getOpcao().getId(), PesoAtribuido::getValor));
    }

    @PutMapping("/clientes/{clienteId}/pesos-atribuidos")
    @Transactional
    public Map<Long, Integer> salvarPesos(@PathVariable Long clienteId,
                                           @RequestBody List<PesoAtribuidoItem> itens) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));
        pesoAtribuidoRepository.deleteByClienteId(clienteId);
        pesoAtribuidoRepository.flush();
        for (PesoAtribuidoItem item : itens) {
            OpcaoResposta opcao = opcaoRespostaRepository.findById(item.opcaoId())
                    .orElseThrow(() -> new EntityNotFoundException("Opção não encontrada: " + item.opcaoId()));
            PesoAtribuido pa = new PesoAtribuido();
            pa.setEmpresa(cliente.getEmpresa());
            pa.setCliente(cliente);
            pa.setOpcao(opcao);
            pa.setValor(item.valor());
            pesoAtribuidoRepository.save(pa);
        }
        return pesosAtribuidos(clienteId);
    }

    @GetMapping("/analises/{id}/parecer")
    public ResponseEntity<byte[]> parecer(@PathVariable Long id) {
        byte[] pdf = parecerPdfService.gerar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition",
                        ContentDisposition.inline().filename("parecer-analise-" + id + ".pdf").toString())
                .body(pdf);
    }
}
