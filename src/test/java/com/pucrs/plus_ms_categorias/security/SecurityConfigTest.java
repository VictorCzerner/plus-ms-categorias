package com.pucrs.plus_ms_categorias.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void deveDecodificarTokenHs256ComSecretDoAuth() throws Exception {
        String token = gerarToken("dev-secret", "admin");

        var jwt = securityConfig.jwtDecoder("dev-secret").decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("admin");
    }

    @Test
    void deveConverterRoleDoAuthParaAuthorityDoSpring() throws Exception {
        String token = gerarToken("dev-secret", "admin");
        var jwt = securityConfig.jwtDecoder("dev-secret").decode(token);

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
    }

    private String gerarToken(String secret, String role) throws Exception {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("""
                {"sub":"admin@example.com","user_id":1,"role":"%s","exp":%d}
                """.formatted(role, Instant.now().plusSeconds(3600).getEpochSecond()));

        String content = header + "." + payload;
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
