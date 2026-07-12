package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Contas granulares de um exercício. Agregados (AC, PC, Ativo Total, grupos do
 * Fleuriet) são sempre derivados no cálculo — nunca digitados.
 */
@Entity
@Table(name = "demonstrativo", uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "exercicio"}))
public class Demonstrativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private int exercicio;

    // DRE
    @Column(name = "receita_bruta", nullable = false, precision = 15, scale = 2)
    private BigDecimal receitaBruta = BigDecimal.ZERO;
    @Column(name = "lucro_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal lucroLiquido = BigDecimal.ZERO;

    // Ativo circulante — financeiro (errático) e operacional (cíclico)
    @Column(name = "caixa_bancos", nullable = false, precision = 15, scale = 2)
    private BigDecimal caixaBancos = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal aplicacoes = BigDecimal.ZERO;
    @Column(name = "contas_receber", nullable = false, precision = 15, scale = 2)
    private BigDecimal contasReceber = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal estoques = BigDecimal.ZERO;
    @Column(name = "outros_ativos_circulantes", nullable = false, precision = 15, scale = 2)
    private BigDecimal outrosAtivosCirculantes = BigDecimal.ZERO;

    // Ativo de longo prazo
    @Column(name = "realizavel_longo_prazo", nullable = false, precision = 15, scale = 2)
    private BigDecimal realizavelLongoPrazo = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal imobilizado = BigDecimal.ZERO;

    // Passivo circulante — financeiro e operacional
    @Column(name = "emprestimos_curto_prazo", nullable = false, precision = 15, scale = 2)
    private BigDecimal emprestimosCurtoPrazo = BigDecimal.ZERO;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal fornecedores = BigDecimal.ZERO;
    @Column(name = "salarios_a_pagar", nullable = false, precision = 15, scale = 2)
    private BigDecimal salariosAPagar = BigDecimal.ZERO;
    @Column(name = "outras_obrigacoes_circulantes", nullable = false, precision = 15, scale = 2)
    private BigDecimal outrasObrigacoesCirculantes = BigDecimal.ZERO;

    // Longo prazo e PL (PL já inclui o resultado do exercício)
    @Column(name = "passivo_nao_circulante", nullable = false, precision = 15, scale = 2)
    private BigDecimal passivoNaoCirculante = BigDecimal.ZERO;
    @Column(name = "patrimonio_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal patrimonioLiquido = BigDecimal.ZERO;

    // ---- Agregados derivados ----

    @Transient
    public BigDecimal getAtivoCirculante() {
        return caixaBancos.add(aplicacoes).add(contasReceber).add(estoques).add(outrosAtivosCirculantes);
    }

    @Transient
    public BigDecimal getPassivoCirculante() {
        return emprestimosCurtoPrazo.add(fornecedores).add(salariosAPagar).add(outrasObrigacoesCirculantes);
    }

    @Transient
    public BigDecimal getAtivoTotal() {
        return getAtivoCirculante().add(realizavelLongoPrazo).add(imobilizado);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public int getExercicio() { return exercicio; }
    public void setExercicio(int exercicio) { this.exercicio = exercicio; }
    public BigDecimal getReceitaBruta() { return receitaBruta; }
    public void setReceitaBruta(BigDecimal v) { this.receitaBruta = v; }
    public BigDecimal getLucroLiquido() { return lucroLiquido; }
    public void setLucroLiquido(BigDecimal v) { this.lucroLiquido = v; }
    public BigDecimal getCaixaBancos() { return caixaBancos; }
    public void setCaixaBancos(BigDecimal v) { this.caixaBancos = v; }
    public BigDecimal getAplicacoes() { return aplicacoes; }
    public void setAplicacoes(BigDecimal v) { this.aplicacoes = v; }
    public BigDecimal getContasReceber() { return contasReceber; }
    public void setContasReceber(BigDecimal v) { this.contasReceber = v; }
    public BigDecimal getEstoques() { return estoques; }
    public void setEstoques(BigDecimal v) { this.estoques = v; }
    public BigDecimal getOutrosAtivosCirculantes() { return outrosAtivosCirculantes; }
    public void setOutrosAtivosCirculantes(BigDecimal v) { this.outrosAtivosCirculantes = v; }
    public BigDecimal getRealizavelLongoPrazo() { return realizavelLongoPrazo; }
    public void setRealizavelLongoPrazo(BigDecimal v) { this.realizavelLongoPrazo = v; }
    public BigDecimal getImobilizado() { return imobilizado; }
    public void setImobilizado(BigDecimal v) { this.imobilizado = v; }
    public BigDecimal getEmprestimosCurtoPrazo() { return emprestimosCurtoPrazo; }
    public void setEmprestimosCurtoPrazo(BigDecimal v) { this.emprestimosCurtoPrazo = v; }
    public BigDecimal getFornecedores() { return fornecedores; }
    public void setFornecedores(BigDecimal v) { this.fornecedores = v; }
    public BigDecimal getSalariosAPagar() { return salariosAPagar; }
    public void setSalariosAPagar(BigDecimal v) { this.salariosAPagar = v; }
    public BigDecimal getOutrasObrigacoesCirculantes() { return outrasObrigacoesCirculantes; }
    public void setOutrasObrigacoesCirculantes(BigDecimal v) { this.outrasObrigacoesCirculantes = v; }
    public BigDecimal getPassivoNaoCirculante() { return passivoNaoCirculante; }
    public void setPassivoNaoCirculante(BigDecimal v) { this.passivoNaoCirculante = v; }
    public BigDecimal getPatrimonioLiquido() { return patrimonioLiquido; }
    public void setPatrimonioLiquido(BigDecimal v) { this.patrimonioLiquido = v; }
}
