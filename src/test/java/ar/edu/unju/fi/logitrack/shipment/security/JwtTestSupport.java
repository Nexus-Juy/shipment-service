package ar.edu.unju.fi.logitrack.shipment.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Firma JWT de prueba con el mismo secreto HS256 que usa {@code security-service}
 * (ver {@code src/test/resources/application.yml}).
 */
public final class JwtTestSupport {

    /** Mismo valor que {@code jwt.secret} en los recursos de test. */
    public static final String SECRETO = "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";

    private JwtTestSupport() {
    }

    /** Token válido por una hora con los roles indicados (por ejemplo {@code "CLIENTE"}). */
    public static String token(String... roles) {
        return firmar(SECRETO, Instant.now().plusSeconds(3_600), roles);
    }

    /** Token ya vencido, para probar el 401. */
    public static String tokenExpirado(String... roles) {
        return firmar(SECRETO, Instant.now().minusSeconds(60), roles, Instant.now().minusSeconds(7_200));
    }

    /** Token firmado con otro secreto, para probar que una firma inválida se rechaza. */
    public static String tokenConOtroSecreto(String... roles) {
        return firmar("OtroSecretoDistintoQueTampocoDeberiaServirParaValidarJWT123456!", Instant.now().plusSeconds(3_600), roles);
    }

    private static String firmar(String secreto, Instant expiracion, String... roles) {
        return firmar(secreto, expiracion, roles, Instant.now().minusSeconds(10));
    }

    private static String firmar(String secreto, Instant expiracion, String[] roles, Instant emision) {
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(clave)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .claim("roles", List.of(roles))
                .issuedAt(emision)
                .expiresAt(expiracion)
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
