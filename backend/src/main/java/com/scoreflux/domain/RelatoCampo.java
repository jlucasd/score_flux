package com.scoreflux.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aba "Relato de Campo": questionário estruturado, um por cliente (upsert).
 * As opções fechadas alimentam os subcritérios qualitativos 1.1, 2.3 e 4.1–4.4.
 */
@Entity
@Table(name = "relato_campo")
public class RelatoCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", unique = true)
    private Cliente cliente;

    @Column(name = "conceito_comercial", length = 40)
    private String conceitoComercial;

    @Column(name = "conceito_comercial_justificativa", length = 2000)
    private String conceitoComercialJustificativa;

    @Column(name = "tempo_mercado", length = 40)
    private String tempoMercado;

    @Column(name = "tempo_mercado_justificativa", length = 2000)
    private String tempoMercadoJustificativa;

    @Column(length = 40)
    private String bandeira;

    @Column(name = "bandeira_justificativa", length = 2000)
    private String bandeiraJustificativa;

    @Column(name = "possui_erp")
    private Boolean possuiErp;

    @Column(name = "possui_cobranca")
    private Boolean possuiCobranca;

    @Column(name = "unidades_negocio", length = 40)
    private String unidadesNegocio;

    @Column(name = "unidades_negocio_justificativa", length = 2000)
    private String unidadesNegocioJustificativa;

    @Column(name = "risco_climatico", length = 40)
    private String riscoClimatico;

    @Column(name = "risco_climatico_justificativa", length = 2000)
    private String riscoClimaticoJustificativa;

    @Column(length = 4000)
    private String observacoes;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public String getConceitoComercial() { return conceitoComercial; }
    public void setConceitoComercial(String conceitoComercial) { this.conceitoComercial = conceitoComercial; }
    public String getConceitoComercialJustificativa() { return conceitoComercialJustificativa; }
    public void setConceitoComercialJustificativa(String conceitoComercialJustificativa) { this.conceitoComercialJustificativa = conceitoComercialJustificativa; }
    public String getTempoMercado() { return tempoMercado; }
    public void setTempoMercado(String tempoMercado) { this.tempoMercado = tempoMercado; }
    public String getTempoMercadoJustificativa() { return tempoMercadoJustificativa; }
    public void setTempoMercadoJustificativa(String tempoMercadoJustificativa) { this.tempoMercadoJustificativa = tempoMercadoJustificativa; }
    public String getBandeira() { return bandeira; }
    public void setBandeira(String bandeira) { this.bandeira = bandeira; }
    public String getBandeiraJustificativa() { return bandeiraJustificativa; }
    public void setBandeiraJustificativa(String bandeiraJustificativa) { this.bandeiraJustificativa = bandeiraJustificativa; }
    public Boolean getPossuiErp() { return possuiErp; }
    public void setPossuiErp(Boolean possuiErp) { this.possuiErp = possuiErp; }
    public Boolean getPossuiCobranca() { return possuiCobranca; }
    public void setPossuiCobranca(Boolean possuiCobranca) { this.possuiCobranca = possuiCobranca; }
    public String getUnidadesNegocio() { return unidadesNegocio; }
    public void setUnidadesNegocio(String unidadesNegocio) { this.unidadesNegocio = unidadesNegocio; }
    public String getUnidadesNegocioJustificativa() { return unidadesNegocioJustificativa; }
    public void setUnidadesNegocioJustificativa(String unidadesNegocioJustificativa) { this.unidadesNegocioJustificativa = unidadesNegocioJustificativa; }
    public String getRiscoClimatico() { return riscoClimatico; }
    public void setRiscoClimatico(String riscoClimatico) { this.riscoClimatico = riscoClimatico; }
    public String getRiscoClimaticoJustificativa() { return riscoClimaticoJustificativa; }
    public void setRiscoClimaticoJustificativa(String riscoClimaticoJustificativa) { this.riscoClimaticoJustificativa = riscoClimaticoJustificativa; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
