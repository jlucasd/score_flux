package com.scoreflux.api;

import com.scoreflux.domain.ItemFluxo;
import com.scoreflux.domain.Mecanismo;
import com.scoreflux.service.FluxoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ItemController {

    private final FluxoService service;

    public ItemController(FluxoService service) {
        this.service = service;
    }

    public record ItemRequest(@NotBlank String descricao,
                              @NotNull Mecanismo mecanismo,
                              String mecanismoOutro,
                              String nota) {
    }

    public record ItemResponse(Long id, String descricao, String mecanismo,
                               String mecanismoOutro, String nota, int ordem) {
        static ItemResponse de(ItemFluxo item) {
            return new ItemResponse(item.getId(), item.getDescricao(), item.getMecanismo().name(),
                    item.getMecanismoOutro(), item.getNota(), item.getOrdem());
        }
    }

    public record MecanismoResponse(String codigo, String rotulo) {
    }

    @GetMapping("/mecanismos")
    public List<MecanismoResponse> mecanismos() {
        return Arrays.stream(Mecanismo.values())
                .map(m -> new MecanismoResponse(m.name(), m.getRotulo()))
                .toList();
    }

    @PostMapping("/planos/{planoId}/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse criar(@PathVariable Long planoId, @Valid @RequestBody ItemRequest request) {
        return ItemResponse.de(service.criarItem(planoId, request.descricao(),
                request.mecanismo(), request.mecanismoOutro(), request.nota()));
    }

    @PutMapping("/itens/{id}")
    public ItemResponse atualizar(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return ItemResponse.de(service.atualizarItem(id, request.descricao(),
                request.mecanismo(), request.mecanismoOutro(), request.nota()));
    }

    @DeleteMapping("/itens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirItem(id);
    }

    /** Corpo: {"1": 1500.00, "2": 0, ...} — mês (1-12) para valor. */
    @PutMapping("/itens/{id}/valores")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salvarValores(@PathVariable Long id, @RequestBody Map<Integer, BigDecimal> valores) {
        service.salvarValores(id, valores);
    }
}
