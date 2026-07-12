package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "valor_mensal", uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "mes"}))
public class ValorMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private ItemFluxo item;

    // 1 = janeiro ... 12 = dezembro
    @Column(nullable = false)
    private int mes;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ItemFluxo getItem() { return item; }
    public void setItem(ItemFluxo item) { this.item = item; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
