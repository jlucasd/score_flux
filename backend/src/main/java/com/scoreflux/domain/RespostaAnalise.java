package com.scoreflux.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "resposta_analise", uniqueConstraints = @UniqueConstraint(columnNames = {"analise_id", "subcriterio_id"}))
public class RespostaAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analise_id")
    private AnaliseCredito analise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcriterio_id")
    private Subcriterio subcriterio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcao_id")
    private OpcaoResposta opcao;

    @Column(length = 2000)
    private String justificativa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AnaliseCredito getAnalise() { return analise; }
    public void setAnalise(AnaliseCredito analise) { this.analise = analise; }
    public Subcriterio getSubcriterio() { return subcriterio; }
    public void setSubcriterio(Subcriterio subcriterio) { this.subcriterio = subcriterio; }
    public OpcaoResposta getOpcao() { return opcao; }
    public void setOpcao(OpcaoResposta opcao) { this.opcao = opcao; }
    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }
}
