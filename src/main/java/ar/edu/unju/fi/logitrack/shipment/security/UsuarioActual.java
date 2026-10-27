package ar.edu.unju.fi.logitrack.shipment.security;

import ar.edu.unju.fi.logitrack.common.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Datos del usuario autenticado extraídos del JWT que emite {@code security-service} (S2-07).
 * <p>
 * El {@code sub} es el UUID del usuario y el claim {@code repartidorId} solo viene cuando el rol es
 * {@code REPARTIDOR}. Se lee del {@link SecurityContextHolder} en cada llamada, así el servicio no
 * depende de parámetros que el controller podría olvidar pasar.
 * </p>
 */
@Component
public class UsuarioActual {

    /** Claim con el UUID del repartidor (solo para el rol REPARTIDOR). */
    public static final String CLAIM_REPARTIDOR_ID = "repartidorId";

    public boolean esAdmin() {
        return tieneRol(Roles.ROLE_ADMIN);
    }

    public boolean esCliente() {
        return tieneRol(Roles.ROLE_CLIENTE);
    }

    public boolean esRepartidor() {
        return tieneRol(Roles.ROLE_REPARTIDOR);
    }

    /** UUID del usuario autenticado (claim {@code sub}); {@code null} si no se puede determinar. */
    public UUID usuarioId() {
        Optional<Jwt> jwt = jwt();
        if (jwt.isPresent()) {
            return parsearUuid(jwt.get().getSubject());
        }
        // En tests con @WithMockUser no hay JWT: el nombre del principal hace de sub.
        return autenticacion().map(Authentication::getName).map(UsuarioActual::parsearUuid).orElse(null);
    }

    /** UUID del repartidor (claim {@code repartidorId}); {@code null} si el token no lo trae. */
    public UUID repartidorId() {
        return jwt().map(jwtActual -> jwtActual.getClaimAsString(CLAIM_REPARTIDOR_ID))
            .map(UsuarioActual::parsearUuid)
            .orElse(null);
    }

    private boolean tieneRol(String rol) {
        return autenticacion()
            .map(Authentication::getAuthorities)
            .map(autoridades -> autoridades.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(rol::equals))
            .orElse(false);
    }

    private Optional<Authentication> autenticacion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.of(autenticacion);
    }

    private Optional<Jwt> jwt() {
        return autenticacion()
            .map(Authentication::getPrincipal)
            .filter(Jwt.class::isInstance)
            .map(Jwt.class::cast);
    }

    private static UUID parsearUuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
