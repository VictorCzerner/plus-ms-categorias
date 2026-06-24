package com.pucrs.plus_ms_categorias.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacSha256JwtDecoderTest {

    private static final String TEST_SECRET = "categorias-test-secret";
    private final HmacSha256JwtDecoder decoder =
            new HmacSha256JwtDecoder(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void shouldAcceptValidHs256TokenAndPreserveClaims() throws Exception {
        long expiration = Instant.now().plusSeconds(3600).getEpochSecond();
        String token = token("HS256", TEST_SECRET,
                payload("admin@example.com", 7, "admin", expiration));

        var jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin@example.com");
        assertThat(((Number) jwt.getClaim("user_id")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("admin");
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.ofEpochSecond(expiration));
    }

    @Test
    void shouldRejectTokenWithoutThreeSegments() {
        assertThatThrownBy(() -> decoder.decode("header.payload"))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("malformado");
    }

    @Test
    void shouldRejectAlgorithmDifferentFromHs256() throws Exception {
        String token = token("HS512", TEST_SECRET,
                payload("admin@example.com", 1, "admin", futureExpiration()));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Algoritmo");
    }

    @Test
    void shouldRejectInvalidSignature() throws Exception {
        String token = token("HS256", "another-secret",
                payload("admin@example.com", 1, "admin", futureExpiration()));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("Assinatura");
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String token = token("HS256", TEST_SECRET,
                payload("admin@example.com", 1, "admin", Instant.now().minusSeconds(1).getEpochSecond()));

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void shouldRejectTokenWithoutExpiration() throws Exception {
        String token = token("HS256", TEST_SECRET,
                "{\"sub\":\"admin@example.com\",\"user_id\":1,\"role\":\"admin\"}");

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("sem expiracao");
    }

    @Test
    void shouldCurrentlyAcceptRefreshTokenWithSameClaimsAsAccessToken() throws Exception {
        String refreshToken = token("HS256", TEST_SECRET,
                payload("admin@example.com", 1, "admin",
                        Instant.now().plusSeconds(7 * 24 * 60 * 60).getEpochSecond()));

        var jwt = decoder.decode(refreshToken);

        assertThat(jwt.getSubject()).isEqualTo("admin@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("admin");
    }

    private long futureExpiration() {
        return Instant.now().plusSeconds(3600).getEpochSecond();
    }

    private String payload(String subject, long userId, String role, long expiration) {
        return """
                {"sub":"%s","user_id":%d,"role":"%s","exp":%d}
                """.formatted(subject, userId, role, expiration);
    }

    private String token(String algorithm, String secret, String payload) throws Exception {
        String header = base64Url("{\"alg\":\"" + algorithm + "\",\"typ\":\"JWT\"}");
        String encodedPayload = base64Url(payload);
        String content = header + "." + encodedPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        return content + "." + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
