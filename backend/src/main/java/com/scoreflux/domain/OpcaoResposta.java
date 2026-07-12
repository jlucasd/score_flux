package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "opcao_resposta")
public class OpcaoResposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcriterio_id")
    private Subcriterio subcriterio;

    @Column(nullable = false, length = 120)
    private String rotulo;

    @Column(nullable = false)
    private int nota;

    @Column(nullable = false)
    private int ordem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subcriterio getSubcriterio() { return subcriterio; }
    public void setSubcriterio(Subcriterio subcriterio) { this.subcriterio = subcriterio; }
    public String getRotulo() { return rotulo; }
    public void setRotulo(String rotulo) { this.rotulo = rotulo; }
    public int getNota() { return nota; }
    public void setNota(int nota) { this.nota = nota; }
    public int getOrdem() { return ordem; }
    public void setOrdem(int ordem) { this.ordem = ordem; }
}
