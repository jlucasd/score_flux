package com.scoreflux.api;

import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.domain.Cliente;
import com.scoreflux.domain.Demonstrativo;
import com.scoreflux.service.CreditoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ClienteController {

    private final CreditoService service;

    public ClienteController(CreditoService service) {
        this.service = service;
    }

    public record ClienteRequest(@NotBlank String nome, String cpfCnpj, Cliente.Tipo tipo,
                                 String municipio, String uf, String telefone,
                                 @jakarta.validation.constraints.Email String email, String endereco) {

        Cliente paraEntidade() {
            Cliente c = new Cliente();
            c.setNome(nome);
            c.setCpfCnpj(cpfCnpj);
            c.setTipo(tipo);
            c.setMunicipio(municipio);
            c.setUf(uf == null || uf.isBlank() ? null : uf.toUpperCase());
            c.setTelefone(telefone);
            c.setEmail(email);
            c.setEndereco(endereco);
            return c;
        }
    }

    public record ClienteResponse(Long id, String nome, String cpfCnpj, String tipo,
                                  String municipio, String uf, String telefone,
                                  String email, String endereco) {
        static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getCpfCnpj(), c.getTipo().name(),
                    c.getMunicipio(), c.getUf(), c.getTelefone(), c.getEmail(), c.getEndereco());
        }
    }

    public record DemonstrativoRequest(
            BigDecimal receitaBruta, BigDecimal lucroLiquido,
            BigDecimal caixaBancos, BigDecimal aplicacoes, BigDecimal contasReceber,
            BigDecimal estoques, BigDecimal outrosAtivosCirculantes,
            BigDecimal realizavelLongoPrazo, BigDecimal imobilizado,
            BigDecimal emprestimosCurtoPrazo, BigDecimal fornecedores, BigDecimal salariosAPagar,
            BigDecimal outrasObrigacoesCirculantes,
            BigDecimal passivoNaoCirculante, BigDecimal patrimonioLiquido) {

        Demonstrativo paraEntidade() {
            Demonstrativo d = new Demonstrativo();
            d.setReceitaBruta(receitaBruta);
            d.setLucroLiquido(lucroLiquido);
            d.setCaixaBancos(caixaBancos);
            d.setAplicacoes(aplicacoes);
            d.setContasReceber(contasReceber);
            d.setEstoques(estoques);
            d.setOutrosAtivosCirculantes(outrosAtivosCirculantes);
            d.setRealizavelLongoPrazo(realizavelLongoPrazo);
            d.setImobilizado(imobilizado);
            d.setEmprestimosCurtoPrazo(emprestimosCurtoPrazo);
            d.setFornecedores(fornecedores);
            d.setSalariosAPagar(salariosAPagar);
            d.setOutrasObrigacoesCirculantes(outrasObrigacoesCirculantes);
            d.setPassivoNaoCirculante(passivoNaoCirculante);
            d.setPatrimonioLiquido(patrimonioLiquido);
            return d;
        }
    }

    public record DemonstrativoResponse(
            Long id, int exercicio,
            BigDecimal receitaBruta, BigDecimal lucroLiquido,
            BigDecimal caixaBancos, BigDecimal aplicacoes, BigDecimal contasReceber,
            BigDecimal estoques, BigDecimal outrosAtivosCirculantes,
            BigDecimal realizavelLongoPrazo, BigDecimal imobilizado,
            BigDecimal emprestimosCurtoPrazo, BigDecimal fornecedores, BigDecimal salariosAPagar,
            BigDecimal outrasObrigacoesCirculantes,
            BigDecimal passivoNaoCirculante, BigDecimal patrimonioLiquido) {

        static DemonstrativoResponse de(Demonstrativo d) {
            return new DemonstrativoResponse(d.getId(), d.getExercicio(),
                    d.getReceitaBruta(), d.getLucroLiquido(),
                    d.getCaixaBancos(), d.getAplicacoes(), d.getContasReceber(),
                    d.getEstoques(), d.getOutrosAtivosCirculantes(),
                    d.getRealizavelLongoPrazo(), d.getImobilizado(),
                    d.getEmprestimosCurtoPrazo(), d.getFornecedores(), d.getSalariosAPagar(),
                    d.getOutrasObrigacoesCirculantes(),
                    d.getPassivoNaoCirculante(), d.getPatrimonioLiquido());
        }
    }

    // ---- Clientes ----

    @GetMapping("/clientes")
    public List<ClienteResponse> listar() {
        return service.listarClientes().stream().map(ClienteResponse::de).toList();
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse criar(@Valid @RequestBody ClienteRequest r) {
        return ClienteResponse.de(service.salvarCliente(null, r.paraEntidade()));
    }

    @PutMapping("/clientes/{id}")
    public ClienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest r) {
        return ClienteResponse.de(service.salvarCliente(id, r.paraEntidade()));
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluirCliente(id);
    }

    // ---- Demonstrativos ----

    @GetMapping("/clientes/{clienteId}/demonstrativos")
    public List<DemonstrativoResponse> demonstrativos(@PathVariable Long clienteId) {
        return service.listarDemonstrativos(clienteId).stream().map(DemonstrativoResponse::de).toList();
    }

    @PutMapping("/clientes/{clienteId}/demonstrativos/{exercicio}")
    public DemonstrativoResponse salvarDemonstrativo(@PathVariable Long clienteId,
                                                     @PathVariable int exercicio,
                                                     @RequestBody DemonstrativoRequest r) {
        return DemonstrativoResponse.de(service.salvarDemonstrativo(clienteId, exercicio, r.paraEntidade()));
    }

    @DeleteMapping("/demonstrativos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirDemonstrativo(@PathVariable Long id) {
        service.excluirDemonstrativo(id);
    }

    @GetMapping("/clientes/{clienteId}/indicadores")
    public IndicadoresDTO indicadores(@PathVariable Long clienteId) {
        return service.indicadores(clienteId);
    }
}
