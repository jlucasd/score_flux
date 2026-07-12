package com.scoreflux.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.scoreflux.api.dto.AnaliseDetalheDTO;
import com.scoreflux.api.dto.IndicadoresDTO;
import com.scoreflux.api.dto.PoliticaDTO;
import com.scoreflux.domain.Cliente;
import com.scoreflux.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gera o parecer de crédito em PDF a partir de uma análise. */
@Service
public class ParecerPdfService {

    private static final Color VERDE = new Color(28, 107, 51);
    private static final Color VERDE_CLARO = new Color(238, 244, 238);
    private static final Color CINZA = new Color(90, 107, 90);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CreditoService creditoService;
    private final ClienteRepository clienteRepository;

    public ParecerPdfService(CreditoService creditoService, ClienteRepository clienteRepository) {
        this.creditoService = creditoService;
        this.clienteRepository = clienteRepository;
    }

    public byte[] gerar(Long analiseId) {
        AnaliseDetalheDTO analise = creditoService.detalhe(analiseId);
        PoliticaDTO politica = creditoService.politicaVigente();
        IndicadoresDTO indicadores = creditoService.indicadores(analise.clienteId());
        Cliente cliente = clienteRepository.findById(analise.clienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 44, 40);
            PdfWriter.getInstance(doc, saida);
            doc.open();

            titulo(doc, analise);
            dadosCliente(doc, cliente);
            resultado(doc, analise);
            indicadores(doc, indicadores);
            respostas(doc, analise, politica);
            observacoes(doc, analise);
            rodape(doc);

            doc.close();
            return saida.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o parecer em PDF", e);
        }
    }

    private void titulo(Document doc, AnaliseDetalheDTO analise) {
        Paragraph t = new Paragraph("Parecer de Análise de Crédito", fonte(18, Font.BOLD, VERDE));
        doc.add(t);
        String sub = "ScoreFlux · Análise #" + analise.id() + " · "
                + ("CONCLUIDA".equals(analise.status()) && analise.concluidaEm() != null
                ? "concluída em " + analise.concluidaEm().format(DATA)
                : "rascunho (valores provisórios)");
        Paragraph p = new Paragraph(sub, fonte(9, Font.NORMAL, CINZA));
        p.setSpacingAfter(12);
        doc.add(p);
        linha(doc);
    }

    private void dadosCliente(Document doc, Cliente c) {
        secao(doc, "Cliente");
        PdfPTable t = tabela(new float[]{1, 2, 1, 2});
        celulaChave(t, "Nome / Razão Social");
        celulaValor(t, c.getNome());
        celulaChave(t, "CPF/CNPJ");
        celulaValor(t, ou(c.getCpfCnpj()));
        celulaChave(t, "Tipo");
        celulaValor(t, c.getTipo().name());
        celulaChave(t, "Município / UF");
        celulaValor(t, ou(c.getMunicipio()) + (c.getUf() != null ? " / " + c.getUf() : ""));
        celulaChave(t, "Telefone");
        celulaValor(t, ou(c.getTelefone()));
        celulaChave(t, "E-mail");
        celulaValor(t, ou(c.getEmail()));
        doc.add(t);
    }

    private void resultado(Document doc, AnaliseDetalheDTO a) {
        secao(doc, "Resultado");
        var r = a.resultado();
        PdfPTable t = tabela(new float[]{1, 1, 1, 1, 1.4f});
        cabecalho(t, "Score", "Rating", "% do rating", "Base (PL + Fat.)", "Limite de crédito");
        celulaDestaque(t, r.score() == null ? "—" : r.score().toPlainString());
        celulaDestaque(t, ou(r.rating()));
        celulaDestaque(t, pct(r.percentualLimite()));
        celulaDestaque(t, r.baseLimite() == null ? "—" : brl(r.baseLimite()));
        celulaDestaque(t, r.limite() == null ? "—" : brl(r.limite()));
        doc.add(t);
    }

    private void indicadores(Document doc, IndicadoresDTO ind) {
        if (ind.exercicios().isEmpty()) return;
        secao(doc, "Indicadores financeiros");

        PdfPTable m = tabela(new float[]{1, 1, 1, 1});
        cabecalho(m, "ROE (média)", "Endividamento", "Liquidez Seca", "Evolução Vendas");
        celulaValor(m, pct(ind.roeMedia()));
        celulaValor(m, pct(ind.endividamentoMedia()));
        celulaValor(m, num(ind.liquidezSecaMedia()));
        celulaValor(m, pct(ind.evolucaoVendas()));
        doc.add(m);

        Paragraph esp = new Paragraph(" ", fonte(4, Font.NORMAL, Color.BLACK));
        doc.add(esp);

        PdfPTable f = tabela(new float[]{1, 1.2f, 1.2f, 1.2f, 1.6f});
        cabecalho(f, "Exercício", "Tesouraria", "NCG", "CDG", "Diagnóstico (Fleuriet)");
        for (IndicadoresDTO.ExercicioDTO ex : ind.exercicios()) {
            celulaValor(f, String.valueOf(ex.exercicio()));
            celulaValor(f, brl(ex.tesouraria()));
            celulaValor(f, brl(ex.ncg()));
            celulaValor(f, brl(ex.cdg()));
            celulaValor(f, ex.tipoFleuriet() + " — " + ex.diagnostico());
        }
        doc.add(f);
    }

    private void respostas(Document doc, AnaliseDetalheDTO analise, PoliticaDTO politica) {
        secao(doc, "Critérios avaliados");

        Map<Long, PoliticaDTO.SubcriterioDTO> subs = new LinkedHashMap<>();
        Map<Long, PoliticaDTO.OpcaoDTO> opcoes = new LinkedHashMap<>();
        for (PoliticaDTO.SubcriterioDTO s : politica.subcriterios()) {
            subs.put(s.id(), s);
            for (PoliticaDTO.OpcaoDTO o : s.opcoes()) opcoes.put(o.id(), o);
        }

        PdfPTable t = tabela(new float[]{0.6f, 2.4f, 1.8f, 2.2f});
        cabecalho(t, "Item", "Critério", "Resposta", "Justificativa");
        for (AnaliseDetalheDTO.RespostaDTO r : analise.respostas()) {
            PoliticaDTO.SubcriterioDTO s = subs.get(r.subcriterioId());
            PoliticaDTO.OpcaoDTO o = opcoes.get(r.opcaoId());
            if (s == null || o == null) continue;
            celulaValor(t, s.codigo());
            celulaValor(t, s.nome());
            celulaValor(t, o.rotulo() + " (" + o.nota() + ")");
            celulaValor(t, ou(r.justificativa()));
        }
        doc.add(t);
    }

    private void observacoes(Document doc, AnaliseDetalheDTO analise) {
        if (analise.observacoes() == null || analise.observacoes().isBlank()) return;
        secao(doc, "Observações");
        Paragraph p = new Paragraph(analise.observacoes(), fonte(10, Font.NORMAL, Color.BLACK));
        p.setSpacingAfter(10);
        doc.add(p);
    }

    private void rodape(Document doc) {
        linha(doc);
        Paragraph p = new Paragraph(
                "Documento gerado pelo ScoreFlux. Metodologia: política de crédito com pesos, "
                        + "rating AAA–H e limite = média(Patrimônio Líquido, Faturamento) × percentual do rating.",
                fonte(7.5f, Font.ITALIC, CINZA));
        doc.add(p);
    }

    // ---- helpers de layout ----

    private void secao(Document doc, String texto) {
        Paragraph p = new Paragraph(texto, fonte(12, Font.BOLD, VERDE));
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void linha(Document doc) {
        doc.add(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100, VERDE, Element.ALIGN_CENTER, -4));
    }

    private PdfPTable tabela(float[] larguras) {
        PdfPTable t = new PdfPTable(larguras);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        return t;
    }

    private void cabecalho(PdfPTable t, String... titulos) {
        for (String titulo : titulos) {
            PdfPCell c = new PdfPCell(new Phrase(titulo, fonte(8.5f, Font.BOLD, Color.WHITE)));
            c.setBackgroundColor(VERDE);
            c.setPadding(5);
            c.setBorderColor(new Color(198, 210, 198));
            t.addCell(c);
        }
    }

    private void celulaChave(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte(9, Font.BOLD, CINZA)));
        c.setBackgroundColor(VERDE_CLARO);
        c.setPadding(5);
        c.setBorderColor(new Color(198, 210, 198));
        t.addCell(c);
    }

    private void celulaValor(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte(9, Font.NORMAL, Color.BLACK)));
        c.setPadding(5);
        c.setBorderColor(new Color(198, 210, 198));
        t.addCell(c);
    }

    private void celulaDestaque(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fonte(12, Font.BOLD, VERDE)));
        c.setPadding(7);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorderColor(new Color(198, 210, 198));
        t.addCell(c);
    }

    private Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamanho, estilo, cor);
    }

    private String ou(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    // Formata sempre em Locale.US (1,234.56) e converte para o padrão brasileiro (1.234,56),
    // independente do locale da máquina.
    private String brl(BigDecimal v) {
        if (v == null) return "—";
        String s = String.format(java.util.Locale.US, "%,.2f", v);
        return "R$ " + s.replace(",", " ").replace(".", ",").replace(" ", ".");
    }

    private String pct(BigDecimal v) {
        if (v == null) return "—";
        return String.format(java.util.Locale.US, "%.1f", v.doubleValue() * 100).replace(".", ",") + "%";
    }

    private String num(BigDecimal v) {
        if (v == null) return "—";
        return String.format(java.util.Locale.US, "%.2f", v).replace(".", ",");
    }
}
