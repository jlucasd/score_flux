package com.scoreflux.api;

import com.scoreflux.domain.Demonstrativo;
import com.scoreflux.domain.DemonstrativoCampoExtra;
import com.scoreflux.repository.DemonstrativoCampoExtraRepository;
import com.scoreflux.repository.DemonstrativoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CampoExtraController {

    private final DemonstrativoCampoExtraRepository campoExtraRepo;
    private final DemonstrativoRepository demonstrativoRepo;

    public CampoExtraController(DemonstrativoCampoExtraRepository campoExtraRepo,
                                DemonstrativoRepository demonstrativoRepo) {
        this.campoExtraRepo = campoExtraRepo;
        this.demonstrativoRepo = demonstrativoRepo;
    }

    public record CampoExtraRequest(@NotBlank String grupo, @NotBlank String nome, BigDecimal valor) {}
    public record CampoExtraResponse(Long id, Long demonstrativoId, String grupo, String nome, BigDecimal valor) {
        static CampoExtraResponse de(DemonstrativoCampoExtra c) {
            return new CampoExtraResponse(c.getId(), c.getDemonstrativo().getId(), c.getGrupo(), c.getNome(), c.getValor());
        }
    }

    @GetMapping("/demonstrativos/{demoId}/campos-extras")
    public List<CampoExtraResponse> listar(@PathVariable Long demoId) {
        return campoExtraRepo.findAllByDemonstrativoId(demoId).stream().map(CampoExtraResponse::de).toList();
    }

    @PostMapping("/demonstrativos/{demoId}/campos-extras")
    @ResponseStatus(HttpStatus.CREATED)
    public CampoExtraResponse criar(@PathVariable Long demoId, @Valid @RequestBody CampoExtraRequest r) {
        Demonstrativo demo = demonstrativoRepo.findById(demoId)
                .orElseThrow(() -> new EntityNotFoundException("Demonstrativo não encontrado: " + demoId));
        DemonstrativoCampoExtra campo = new DemonstrativoCampoExtra();
        campo.setDemonstrativo(demo);
        campo.setGrupo(r.grupo());
        campo.setNome(r.nome());
        campo.setValor(r.valor() != null ? r.valor() : BigDecimal.ZERO);
        return CampoExtraResponse.de(campoExtraRepo.save(campo));
    }

    @PutMapping("/campos-extras/{id}")
    public CampoExtraResponse atualizar(@PathVariable Long id, @RequestBody CampoExtraRequest r) {
        DemonstrativoCampoExtra campo = campoExtraRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campo extra não encontrado: " + id));
        if (r.nome() != null) campo.setNome(r.nome());
        if (r.valor() != null) campo.setValor(r.valor());
        return CampoExtraResponse.de(campoExtraRepo.save(campo));
    }

    @DeleteMapping("/campos-extras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        campoExtraRepo.deleteById(id);
    }
}
