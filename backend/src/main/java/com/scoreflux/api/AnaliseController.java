package com.scoreflux.api;

import com.scoreflux.api.dto.AnaliseDetalheDTO;
import com.scoreflux.api.dto.PoliticaDTO;
import com.scoreflux.domain.AnaliseCredito;
import com.scoreflux.service.CreditoService;
import com.scoreflux.service.ParecerPdfService;
import com.scoreflux.service.ScoreCalculator;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AnaliseController {

    private final CreditoService service;
    private final ParecerPdfService parecerPdfService;

    public AnaliseController(CreditoService service, ParecerPdfService parecerPdfService) {
        this.service = service;
        this.parecerPdfService = parecerPdfService;
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
