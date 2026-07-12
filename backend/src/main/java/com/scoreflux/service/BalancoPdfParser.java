package com.scoreflux.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai contas de um balanço/balancete em PDF por correspondência de nomes de conta
 * (formato numérico brasileiro: 1.234.567,89; parênteses = negativo).
 * A extração é uma SUGESTÃO — o usuário confere os valores na tela antes de salvar.
 */
@Service
public class BalancoPdfParser {

    public record Resultado(Map<String, BigDecimal> campos, Integer exercicioDetectado) {
    }

    /** Sinônimos por campo (normalizados: minúsculas, sem acento). Primeira ocorrência vence. */
    private static final Map<String, List<String>> SINONIMOS = new LinkedHashMap<>();

    static {
        SINONIMOS.put("caixaBancos", List.of("caixa e bancos", "caixa e equivalentes", "caixa"));
        SINONIMOS.put("aplicacoes", List.of("aplicacoes financeiras", "aplicacoes/inve", "aplicacoes"));
        SINONIMOS.put("contasReceber", List.of("duplicatas a receber", "contas a receber", "clientes"));
        SINONIMOS.put("estoques", List.of("estoques", "estoque"));
        SINONIMOS.put("outrosAtivosCirculantes", List.of("outras apropriacoes", "outros ativos circulantes"));
        SINONIMOS.put("realizavelLongoPrazo", List.of("realizavel a longo prazo", "real.lg.prazo", "realizavel lg"));
        SINONIMOS.put("imobilizado", List.of("imobilizado"));
        SINONIMOS.put("fornecedores", List.of("fornecedores"));
        SINONIMOS.put("salariosAPagar", List.of("salarios a pagar", "salarios a pg", "obrigacoes sociais e trabalhistas"));
        SINONIMOS.put("outrasObrigacoesCirculantes", List.of("outras obrigacoes"));
        SINONIMOS.put("passivoNaoCirculante", List.of("passivo nao circulante", "exigivel a longo prazo"));
        SINONIMOS.put("patrimonioLiquido", List.of("patrimonio liquido"));
        SINONIMOS.put("lucroLiquido", List.of("lucro liquido", "lucro/prejuizo", "lucro ou prejuizo"));
        SINONIMOS.put("receitaBruta", List.of("receita operacional bruta", "receita bruta", "faturamento bruto"));
        // Por último: "emprestimos" também aparece no longo prazo — só preenche se ainda vazio
        SINONIMOS.put("emprestimosCurtoPrazo", List.of("emprestimos e financiamentos", "emprestimos"));
    }

    private static final Pattern NUMERO = Pattern.compile("\\(?-?[\\d]{1,3}(?:[.\\d]{0,15})?(?:,\\d{1,2})?\\)?");
    private static final Pattern ANO = Pattern.compile("\\b(20\\d{2})\\b");

    public Resultado extrair(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(doc);
            return extrairDeTexto(texto);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o PDF — verifique se o arquivo é válido");
        }
    }

    public Resultado extrairDeTexto(String texto) {
        Map<String, BigDecimal> campos = new LinkedHashMap<>();
        Integer exercicio = null;

        for (String linhaOriginal : texto.split("\\r?\\n")) {
            String linha = normalizar(linhaOriginal);

            if (exercicio == null) {
                Matcher ano = ANO.matcher(linha);
                if (ano.find()) exercicio = Integer.parseInt(ano.group(1));
            }

            for (Map.Entry<String, List<String>> entrada : SINONIMOS.entrySet()) {
                if (campos.containsKey(entrada.getKey())) continue;
                boolean corresponde = entrada.getValue().stream().anyMatch(linha::contains);
                if (!corresponde) continue;
                BigDecimal valor = ultimoNumero(linhaOriginal);
                if (valor != null) campos.put(entrada.getKey(), valor);
            }
        }
        return new Resultado(campos, exercicio);
    }

    /** Último token numérico da linha (o valor da conta fica no fim, como na planilha). */
    private BigDecimal ultimoNumero(String linha) {
        Matcher m = NUMERO.matcher(linha.replace("R$", " "));
        String ultimo = null;
        while (m.find()) {
            String token = m.group();
            // Ignora tokens que são apenas ano/código curto sem separador decimal
            if (token.replaceAll("[^\\d]", "").length() < 1) continue;
            ultimo = token;
        }
        if (ultimo == null) return null;

        boolean negativo = ultimo.startsWith("(") || ultimo.startsWith("-");
        String limpo = ultimo.replaceAll("[()\\-]", "");
        // Formato BR: pontos são milhar quando há vírgula; sem vírgula, pontos também são milhar
        limpo = limpo.contains(",")
                ? limpo.replace(".", "").replace(",", ".")
                : limpo.replace(".", "");
        if (limpo.isBlank()) return null;
        try {
            BigDecimal valor = new BigDecimal(limpo);
            return negativo ? valor.negate() : valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }
}
