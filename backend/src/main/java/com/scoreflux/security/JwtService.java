package com.scoreflux.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey chave;
    private final Duration validade = Duration.ofHours(12);

    public JwtService(@Value("${scoreflux.jwt.secret:scoreflux-dev-secret-trocar-em-producao-0123456789}") String segredo) {
        // Deriva uma chave de 512 bits via SHA-512, para qualquer segredo funcionar com HS384/512
        // (evita WeakKeyException se o segredo injetado em produção for curto).
        this.chave = new SecretKeySpec(sha512(segredo), "HmacSHA384");
    }

    private static byte[] sha512(String segredo) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(segredo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 indisponível", e);
        }
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
