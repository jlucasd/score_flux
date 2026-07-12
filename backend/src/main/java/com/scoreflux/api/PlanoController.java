package com.scoreflux.api;

import com.scoreflux.api.dto.ResumoPlanoDTO;
import com.scoreflux.domain.PlanoFluxo;
import com.scoreflux.service.FluxoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

    private final FluxoService service;

    public PlanoController(FluxoService service) {
        this.service = service;
    }

    public record PlanoRequest(@NotBlank String nome, @Min(2000) @Max(2100) int ano, String uf, Long clienteId) {
    }

    public record PlanoResponse(Long id, String nome, int ano, String uf, Long clienteId, String clienteNome) {
        static PlanoResponse de(PlanoFluxo plano) {
            return new PlanoResponse(plano.getId(), plano.getNome(), plano.getAno(), plano.getUf(),
                    plano.getCliente() == null ? null : plano.getCliente().getId(),
                    plano.getCliente() == null ? null : plano.getCliente().getNome());
        }
    }

    @GetMapping
    public List<PlanoResponse> listar(@RequestParam(required = false) String uf) {
        return service.listarPlanos(uf).stream().map(PlanoResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoResponse criar(@Valid @RequestBody PlanoRequest request) {
        return PlanoResponse.de(service.criarPlano(request.nome(), request.ano(), request.uf(), request.clienteId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirPlano(id);
    }

    @GetMapping("/{id}/resumo")
    public ResumoPlanoDTO resumo(@PathVariable Long id) {
        return service.resumo(id);
    }
}
