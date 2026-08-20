package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "peso_atribuido", uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "opcao_id"}))
public class PesoAtribuido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id")
    private OpcaoResposta opcao;

    @Column(nullable = false)
    private int valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public OpcaoResposta getOpcao() { return opcao; }
    public void setOpcao(OpcaoResposta opcao) { this.opcao = opcao; }
    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }
}
