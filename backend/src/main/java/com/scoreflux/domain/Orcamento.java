package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id")
    private PlanoFluxo plano;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    @Column(nullable = false)
    private int quantidade = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlanoFluxo getPlano() { return plano; }
    public void setPlano(PlanoFluxo plano) { this.plano = plano; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}
