package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lançamento de caixa diário. Une o "Extrato Bancário" (realizado) e as
 * "Contas Receber e Pagar" (previsto) da planilha — a diferença é o status.
 * O saldo corrente é sempre derivado, nunca persistido.
 */
@Entity
@Table(name = "lancamento_caixa")
public class LancamentoCaixa {

    public enum Status { REALIZADO, PREVISTO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate data;

    @Column(length = 120)
    private String classificacao;

    @Column(length = 300)
    private String historico;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal entrada = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saida = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.REALIZADO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getClassificacao() { return classificacao; }
    public void setClassificacao(String classificacao) { this.classificacao = classificacao; }
    public String getHistorico() { return historico; }
    public void setHistorico(String historico) { this.historico = historico; }
    public BigDecimal getEntrada() { return entrada; }
    public void setEntrada(BigDecimal entrada) { this.entrada = entrada; }
    public BigDecimal getSaida() { return saida; }
    public void setSaida(BigDecimal saida) { this.saida = saida; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
