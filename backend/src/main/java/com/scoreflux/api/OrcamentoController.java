package com.scoreflux.api;

import com.scoreflux.domain.Orcamento;
import com.scoreflux.service.FluxoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class OrcamentoController {

    private final FluxoService service;

    public OrcamentoController(FluxoService service) {
        this.service = service;
    }

    public record OrcamentoRequest(@NotBlank String descricao,
                                   @NotNull @Min(0) BigDecimal valorUnitario,
                                   @Min(1) int quantidade) {
    }

    public record OrcamentoResponse(Long id, String descricao, BigDecimal valorUnitario, int quantidade) {
        static OrcamentoResponse de(Orcamento orcamento) {
            return new OrcamentoResponse(orcamento.getId(), orcamento.getDescricao(),
                    orcamento.getValorUnitario(), orcamento.getQuantidade());
        }
    }

    @PostMapping("/planos/{planoId}/orcamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@PathVariable Long planoId, @Valid @RequestBody OrcamentoRequest request) {
        return OrcamentoResponse.de(service.criarOrcamento(planoId, request.descricao(),
                request.valorUnitario(), request.quantidade()));
    }

    @DeleteMapping("/orcamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirOrcamento(id);
    }
}
