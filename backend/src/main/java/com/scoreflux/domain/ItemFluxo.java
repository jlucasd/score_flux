package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "item_fluxo")
public class ItemFluxo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id")
    private PlanoFluxo plano;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Mecanismo mecanismo;

    // Preenchido apenas quando mecanismo = OUTRO
    @Column(name = "mecanismo_outro", length = 100)
    private String mecanismoOutro;

    @Column(length = 2000)
    private String nota;

    @Column(nullable = false)
    private int ordem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PlanoFluxo getPlano() { return plano; }
    public void setPlano(PlanoFluxo plano) { this.plano = plano; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Mecanismo getMecanismo() { return mecanismo; }
    public void setMecanismo(Mecanismo mecanismo) { this.mecanismo = mecanismo; }
    public String getMecanismoOutro() { return mecanismoOutro; }
    public void setMecanismoOutro(String mecanismoOutro) { this.mecanismoOutro = mecanismoOutro; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
