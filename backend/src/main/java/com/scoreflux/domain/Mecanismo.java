package com.scoreflux.domain;

public enum Mecanismo {
    TED("TED"),
    PIX("PIX"),
    BOLETO("Boleto"),
    FINANCIAMENTO("Financiamento"),
    LEASING("Leasing"),
    INTEGRALIZACAO("Integralização"),
    DINHEIRO("Dinheiro"),
    OUTRO("Outro");

    private final String rotulo;

    Mecanismo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
