package com.example.delivery.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "test-only-secret-with-at-least-256-bits-of-material";

    private final JwtService jwtService = new JwtService(SECRET, 30);

    @Test
    void generatedTokenRoundTripsTheAuthenticatedIdentity() {
        String token = jwtService.generateToken(42L, "alice", "CUSTOMER");

        UserPrincipal principal = jwtService.parseToken(token);

        assertThat(principal).isEqualTo(new UserPrincipal(42L, "alice", "CUSTOMER"));
        assertThat(principal.authorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(30 * 60L);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService("another-secure-secret-with-at-least-256-bits", 5)
                .generateToken(1L, "mallory", "ADMIN");

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void configurationMustProvideStrongKeyMaterial() {
        assertThatThrownBy(() -> new JwtService("short", 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void configurationMustProvidePositiveLifetime() {
        assertThatThrownBy(() -> new JwtService(SECRET, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
