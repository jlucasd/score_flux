package com.scoreflux.api;

import com.scoreflux.api.dto.ExtratoDTO;
import com.scoreflux.domain.LancamentoCaixa;
import com.scoreflux.service.CaixaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    public record LancamentoRequest(
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            String classificacao,
            String historico,
            BigDecimal entrada,
            BigDecimal saida,
            LancamentoCaixa.Status status,
            Long clienteId) {
    }

    @GetMapping
    public ExtratoDTO extrato(@RequestParam(required = false) LancamentoCaixa.Status status) {
        return service.extrato(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExtratoDTO criar(@Valid @RequestBody LancamentoRequest r,
                            @RequestParam(required = false) LancamentoCaixa.Status filtro) {
        service.criar(r.data(), r.classificacao(), r.historico(), r.entrada(), r.saida(), r.status(), r.clienteId());
        return service.extrato(filtro);
    }

    @DeleteMapping("/{id}")
    public ExtratoDTO excluir(@PathVariable Long id,
                              @RequestParam(required = false) LancamentoCaixa.Status filtro) {
        service.excluir(id);
        return service.extrato(filtro);
    }
}
