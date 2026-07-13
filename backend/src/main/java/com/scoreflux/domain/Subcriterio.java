package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "subcriterio")
public class Subcriterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_id")
    private PoliticaCredito politica;

    @Column(nullable = false, length = 60)
    private String grupo;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal peso;

    // true = subcritério 3.x, sugerido automaticamente a partir dos demonstrativos
    @Column(nullable = false)
    private boolean automatico;

    @Column(nullable = false)
    private int ordem;

    // Metadados da aba "Parâmetros" (instrumento de coleta, fonte, quem valida, descrição)
    @Column(length = 60)
    private String instrumento;

    @Column(length = 60)
    private String fonte;

    @Column(length = 60)
    private String validacao;

    @Column(length = 200)
    private String descricao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PoliticaCredito getPolitica() { return politica; }
    public void setPolitica(PoliticaCredito politica) { this.politica = politica; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    public boolean isAutomatico() { return automatico; }
    public void setAutomatico(boolean automatico) { this.automatico = automatico; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
    public String getInstrumento() { return instrumento; }
    public void setInstrumento(String instrumento) { this.instrumento = instrumento; }
    public String getFonte() { return fonte; }
    public void setFonte(String fonte) { this.fonte = fonte; }
    public String getValidacao() { return validacao; }
    public void setValidacao(String validacao) { this.validacao = validacao; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
