package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "plano_fluxo")
public class PlanoFluxo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private int ano;

    @Column(length = 2)
    private String uf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
}
