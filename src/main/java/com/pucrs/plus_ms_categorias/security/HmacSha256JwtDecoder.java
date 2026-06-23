package com.pucrs.plus_ms_categorias.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class HmacSha256JwtDecoder implements JwtDecoder {

    private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() {
    };

    private final byte[] secret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    HmacSha256JwtDecoder(byte[] secret) {
        this.secret = secret.clone();
    }

    @Override
    public Jwt decode(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new BadJwtException("Token JWT malformado.");
        }

        try {
            Map<String, Object> headers = decodeJson(parts[0]);
            Map<String, Object> claims = decodeJson(parts[1]);

            if (!"HS256".equals(headers.get("alg"))) {
                throw new BadJwtException("Algoritmo JWT nao suportado.");
            }

            validateSignature(parts);

            Instant issuedAt = toInstant(claims.get("iat"));
            Instant expiresAt = toInstant(claims.get("exp"));
            if (expiresAt == null) {
                throw new BadJwtException("Token JWT sem expiracao.");
            }
            if (Instant.now().isAfter(expiresAt)) {
                throw new BadJwtException("Token JWT expirado.");
            }

            return new Jwt(token, issuedAt, expiresAt, headers, claims);
        } catch (BadJwtException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadJwtException("Token JWT invalido.", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) throws JacksonException {
        String json = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        return objectMapper.readValue(json, JSON_MAP);
    }

    private void validateSignature(String[] parts) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));

        byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        byte[] actual = Base64.getUrlDecoder().decode(parts[2]);

        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BadJwtException("Assinatura JWT invalida.");
        }
    }

    private Instant toInstant(Object value) {
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }

        return null;
    }
}
