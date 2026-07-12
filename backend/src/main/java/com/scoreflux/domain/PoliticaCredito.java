package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "politica_credito")
public class PoliticaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false)
    private int versao;

    @Column(nullable = false, length = 120)
    private String nome;

    // Limiar do subcritério 3.1 (Evolução das Vendas): crescimento acima da inflação
    @Column(name = "inflacao_referencia", nullable = false, precision = 6, scale = 2)
    private BigDecimal inflacaoReferencia;

    @Column(nullable = false)
    private boolean vigente = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public int getVersao() { return versao; }
    public void setVersao(int versao) { this.versao = versao; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getInflacaoReferencia() { return inflacaoReferencia; }
    public void setInflacaoReferencia(BigDecimal v) { this.inflacaoReferencia = v; }
    public boolean isVigente() { return vigente; }
    public void setVigente(boolean vigente) { this.vigente = vigente; }
}
