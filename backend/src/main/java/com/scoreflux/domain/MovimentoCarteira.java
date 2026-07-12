package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimento da carteira de crédito de um cliente: FATURAMENTO aumenta o saldo
 * em aberto; PAGAMENTO reduz. Saldo e disponível são derivados.
 */
@Entity
@Table(name = "movimento_carteira")
public class MovimentoCarteira {

    public enum Tipo { FATURAMENTO, PAGAMENTO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor = BigDecimal.ZERO;

    @Column(length = 300)
    private String descricao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
