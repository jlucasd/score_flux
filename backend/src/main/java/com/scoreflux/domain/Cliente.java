package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "cliente")
public class Cliente {

    public enum Tipo { PRODUTOR, REVENDA, COOPERATIVA, OUTRO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(name = "cpf_cnpj", length = 20)
    private String cpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo = Tipo.PRODUTOR;

    @Column(length = 120)
    private String municipio;

    @Column(length = 2)
    private String uf;

    @Column(length = 30)
    private String telefone;

    @Column(length = 180)
    private String email;

    @Column(length = 300)
    private String endereco;

    @Column(length = 15)
    private String cep;

    @Column(length = 120)
    private String bairro;

    @Column(length = 20)
    private String numero;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
}
