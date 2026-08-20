package com.scoreflux.api;

import com.scoreflux.api.dto.CarteiraDTO;
import com.scoreflux.domain.Cliente;
import com.scoreflux.domain.MovimentoCarteira;
import com.scoreflux.repository.ClienteRepository;
import com.scoreflux.service.CarteiraService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/carteira")
public class CarteiraController {

    private final CarteiraService service;
    private final ClienteRepository clienteRepository;

    public CarteiraController(CarteiraService service, ClienteRepository clienteRepository) {
        this.service = service;
        this.clienteRepository = clienteRepository;
    }

    public record PrazoRequest(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prazo) {}


    public record MovimentoRequest(
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @NotNull MovimentoCarteira.Tipo tipo,
            @NotNull BigDecimal valor,
            String descricao) {
    }

    @GetMapping
    public CarteiraDTO carteira() {
        return service.carteira();
    }

    @GetMapping("/clientes/{clienteId}/movimentos")
    public List<CarteiraDTO.MovimentoDTO> movimentos(@PathVariable Long clienteId) {
        return service.movimentos(clienteId);
    }

    @PostMapping("/clientes/{clienteId}/movimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public CarteiraDTO adicionar(@PathVariable Long clienteId, @Valid @RequestBody MovimentoRequest r) {
        service.adicionarMovimento(clienteId, r.data(), r.tipo(), r.valor(), r.descricao());
        return service.carteira();
    }

    @DeleteMapping("/movimentos/{id}")
    public CarteiraDTO excluir(@PathVariable Long id) {
        service.excluirMovimento(id);
        return service.carteira();
    }

    @PutMapping("/clientes/{clienteId}/prazo")
    public CarteiraDTO atualizarPrazo(@PathVariable Long clienteId, @RequestBody PrazoRequest r) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado: " + clienteId));
        cliente.setPrazoCredito(r.prazo());
        clienteRepository.save(cliente);
        return service.carteira();
    }
}
