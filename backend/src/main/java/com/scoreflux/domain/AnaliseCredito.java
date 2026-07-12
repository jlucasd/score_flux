package com.scoreflux.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analise_credito")
public class AnaliseCredito {

    public enum Status { RASCUNHO, CONCLUIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_id")
    private PoliticaCredito politica;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RASCUNHO;

    @Column(length = 4000)
    private String observacoes;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(length = 3)
    private String rating;

    @Column(name = "percentual_limite", precision = 6, scale = 4)
    private BigDecimal percentualLimite;

    @Column(name = "base_limite", precision = 15, scale = 2)
    private BigDecimal baseLimite;

    @Column(name = "limite_calculado", precision = 15, scale = 2)
    private BigDecimal limiteCalculado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public PoliticaCredito getPolitica() { return politica; }
    public void setPolitica(PoliticaCredito politica) { this.politica = politica; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public void setCriadaEm(LocalDateTime criadaEm) { this.criadaEm = criadaEm; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime concluidaEm) { this.concluidaEm = concluidaEm; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public BigDecimal getPercentualLimite() { return percentualLimite; }
    public void setPercentualLimite(BigDecimal v) { this.percentualLimite = v; }
    public BigDecimal getBaseLimite() { return baseLimite; }
    public void setBaseLimite(BigDecimal v) { this.baseLimite = v; }
    public BigDecimal getLimiteCalculado() { return limiteCalculado; }
    public void setLimiteCalculado(BigDecimal v) { this.limiteCalculado = v; }
}
