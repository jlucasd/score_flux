package com.scoreflux.api;

import com.scoreflux.service.BalancoPdfParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ImportacaoController {

    private final BalancoPdfParser parser;

    public ImportacaoController(BalancoPdfParser parser) {
        this.parser = parser;
    }

    public record ExtracaoResponse(Map<String, BigDecimal> campos,
                                   Set<String> camposEncontrados,
                                   Integer exercicioDetectado) {
    }

    /** Extrai contas de um balanço em PDF. Não persiste nada — a tela preenche o formulário para revisão. */
    @PostMapping(value = "/demonstrativos/extrair-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtracaoResponse extrair(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        if (arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo PDF");
        }
        BalancoPdfParser.Resultado resultado = parser.extrair(arquivo.getBytes());
        return new ExtracaoResponse(resultado.campos(), resultado.campos().keySet(), resultado.exercicioDetectado());
    }
}
