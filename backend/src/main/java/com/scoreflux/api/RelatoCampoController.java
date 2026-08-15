package com.scoreflux.api;

import com.scoreflux.domain.RelatoCampo;
import com.scoreflux.service.CreditoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RelatoCampoController {

    private final CreditoService service;

    public RelatoCampoController(CreditoService service) {
        this.service = service;
    }

    public record RelatoCampoRequest(
            String titulo,
            String conceitoComercial, String conceitoComercialJustificativa,
            String tempoMercado, String tempoMercadoJustificativa,
            String bandeira, String bandeiraJustificativa,
            Boolean possuiErp, Boolean possuiCobranca,
            String unidadesNegocio, String unidadesNegocioJustificativa,
            String riscoClimatico, String riscoClimaticoJustificativa,
            String observacoes) {

        RelatoCampo paraEntidade() {
            RelatoCampo r = new RelatoCampo();
            r.setTitulo(titulo);
            r.setConceitoComercial(conceitoComercial);
            r.setConceitoComercialJustificativa(conceitoComercialJustificativa);
            r.setTempoMercado(tempoMercado);
            r.setTempoMercadoJustificativa(tempoMercadoJustificativa);
            r.setBandeira(bandeira);
            r.setBandeiraJustificativa(bandeiraJustificativa);
            r.setPossuiErp(possuiErp);
            r.setPossuiCobranca(possuiCobranca);
            r.setUnidadesNegocio(unidadesNegocio);
            r.setUnidadesNegocioJustificativa(unidadesNegocioJustificativa);
            r.setRiscoClimatico(riscoClimatico);
            r.setRiscoClimaticoJustificativa(riscoClimaticoJustificativa);
            r.setObservacoes(observacoes);
            return r;
        }
    }

    public record RelatoCampoResponse(
            Long id, Long clienteId, String clienteNome, String clienteCpfCnpj,
            String titulo,
            String conceitoComercial, String conceitoComercialJustificativa,
            String tempoMercado, String tempoMercadoJustificativa,
            String bandeira, String bandeiraJustificativa,
            Boolean possuiErp, Boolean possuiCobranca,
            String unidadesNegocio, String unidadesNegocioJustificativa,
            String riscoClimatico, String riscoClimaticoJustificativa,
            String observacoes, LocalDateTime atualizadoEm) {

        static RelatoCampoResponse de(RelatoCampo r) {
            return new RelatoCampoResponse(
                    r.getId(),
                    r.getCliente().getId(), r.getCliente().getNome(), r.getCliente().getCpfCnpj(),
                    r.getTitulo(),
                    r.getConceitoComercial(), r.getConceitoComercialJustificativa(),
                    r.getTempoMercado(), r.getTempoMercadoJustificativa(),
                    r.getBandeira(), r.getBandeiraJustificativa(),
                    r.getPossuiErp(), r.getPossuiCobranca(),
                    r.getUnidadesNegocio(), r.getUnidadesNegocioJustificativa(),
                    r.getRiscoClimatico(), r.getRiscoClimaticoJustificativa(),
                    r.getObservacoes(), r.getAtualizadoEm());
        }
    }

    @GetMapping("/clientes/{clienteId}/relatos")
    public List<RelatoCampoResponse> listar(@PathVariable Long clienteId) {
        return service.listarRelatos(clienteId).stream().map(RelatoCampoResponse::de).toList();
    }

    @GetMapping("/clientes/{clienteId}/relato-campo")
    public RelatoCampoResponse buscar(@PathVariable Long clienteId) {
        return RelatoCampoResponse.de(service.relatoCampo(clienteId));
    }

    @PostMapping("/clientes/{clienteId}/relatos")
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoCampoResponse criar(@PathVariable Long clienteId,
                                     @RequestBody RelatoCampoRequest r) {
        return RelatoCampoResponse.de(service.criarRelatoCampo(clienteId, r.paraEntidade()));
    }

    @PutMapping("/relatos/{id}")
    public RelatoCampoResponse atualizar(@PathVariable Long id,
                                         @RequestBody RelatoCampoRequest r) {
        return RelatoCampoResponse.de(service.salvarRelatoCampo(id, r.paraEntidade()));
    }

    @DeleteMapping("/relatos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirRelato(id);
    }
}
