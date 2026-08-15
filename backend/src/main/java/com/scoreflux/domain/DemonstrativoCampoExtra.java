package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "demonstrativo_campo_extra")
public class DemonstrativoCampoExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demonstrativo_id")
    private Demonstrativo demonstrativo;

    @Column(nullable = false, length = 60)
    private String grupo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Demonstrativo getDemonstrativo() { return demonstrativo; }
    public void setDemonstrativo(Demonstrativo demonstrativo) { this.demonstrativo = demonstrativo; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
