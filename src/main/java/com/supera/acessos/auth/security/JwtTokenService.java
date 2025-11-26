package com.supera.acessos.auth.security;

import com.supera.acessos.usuario.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expirationMillis;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("departamento", usuario.getDepartamento().name())
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getExpiracao() {
        Instant exp = Instant.now().plusMillis(expirationMillis);
        return DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(exp);
    }

    public String validarTokenEObterSubject(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
