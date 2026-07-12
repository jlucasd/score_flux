package com.scoreflux.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final Duration validade = Duration.ofHours(12);

    public JwtService(@Value("${scoreflux.jwt.secret:scoreflux-dev-secret-trocar-em-producao-0123456789}") String segredo) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(String email) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validade.toMillis()))
                .signWith(chave)
                .compact();
    }

    /** Retorna o e-mail do token, ou null se inválido/expirado. */
    public String validar(String token) {
        try {
            return Jwts.parser().verifyWith(chave).build()
                    .parseSignedClaims(token).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
