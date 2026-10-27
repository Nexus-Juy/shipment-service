package ar.edu.unju.fi.logitrack.shipment.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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
 * <p>
 * Reproduce los claims reales: {@code sub} (UUID del usuario), {@code roles} (lista) y
 * {@code repartidorId} cuando el token es de un repartidor.
 * </p>
 */
public final class JwtTestSupport {

    /** Mismo valor que {@code jwt.secret} en los recursos de test. */
    public static final String SECRETO = "ClaveSecretaDeTestSuperSeguraParaTestsUnitariosLogiTrack2026!";

    private JwtTestSupport() {
    }

    /** Token válido por una hora con los roles indicados (por ejemplo {@code "CLIENTE"}). */
    public static String token(String... roles) {
        return firmar(SECRETO, Instant.now().plusSeconds(3_600), UUID.randomUUID(), null, roles);
    }

    /** Token de un usuario concreto, para probar la pertenencia de los envíos (S2-07). */
    public static String tokenDe(UUID usuarioId, String... roles) {
        return firmar(SECRETO, Instant.now().plusSeconds(3_600), usuarioId, null, roles);
    }

    /** Token de un repartidor: incluye el claim {@code repartidorId} que usa shipment-service. */
    public static String tokenDeRepartidor(UUID repartidorId, String... roles) {
        return firmar(SECRETO, Instant.now().plusSeconds(3_600), UUID.randomUUID(), repartidorId, roles);
    }

    /** Token ya vencido, para probar el 401. */
    public static String tokenExpirado(String... roles) {
        return firmar(SECRETO, Instant.now().minusSeconds(60), UUID.randomUUID(), null, roles,
                Instant.now().minusSeconds(7_200));
    }

    /** Token firmado con otro secreto, para probar que una firma inválida se rechaza. */
    public static String tokenConOtroSecreto(String... roles) {
        return firmar("OtroSecretoDistintoQueTampocoDeberiaServirParaValidarJWT123456!",
                Instant.now().plusSeconds(3_600), UUID.randomUUID(), null, roles);
    }

    /** Decodifica un token de prueba a {@link Jwt}, útil para tests unitarios de autorización. */
    public static Jwt decodificar(String token) {
        SecretKey clave = new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        return decoder.decode(token);
    }

    private static String firmar(String secreto, Instant expiracion, UUID usuarioId, UUID repartidorId,
                                 String... roles) {
        return firmar(secreto, expiracion, usuarioId, repartidorId, roles, Instant.now().minusSeconds(10));
    }

    private static String firmar(String secreto, Instant expiracion, UUID usuarioId, UUID repartidorId,
                                 String[] roles, Instant emision) {
        SecretKey clave = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(clave)
                .algorithm(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(usuarioId.toString())
                .claim("roles", List.of(roles))
                .issuedAt(emision)
                .expiresAt(expiracion);

        if (repartidorId != null) {
            claims.claim("repartidorId", repartidorId.toString());
        }

        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }
}
