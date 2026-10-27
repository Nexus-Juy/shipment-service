package ar.edu.unju.fi.logitrack.shipment.security;

import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.exception.AccesoDenegadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Autorización por <b>pertenencia</b> del recurso (S2-07), complementaria a la autorización por rol
 * que resuelve el resource server.
 * <ul>
 *   <li>{@code ADMIN}: acceso total.</li>
 *   <li>{@code CLIENTE}: solo los envíos de su cliente (el {@code sub} del token debe coincidir con
 *       el {@code usuarioId} del cliente).</li>
 *   <li>{@code REPARTIDOR}: solo los envíos asignados a su {@code repartidorId}.</li>
 * </ul>
 * Cuando la regla no se cumple se lanza {@link AccesoDenegadoException}, que el handler global
 * traduce a un 403 en formato {@code ProblemDetail}.
 */
@Component
@RequiredArgsConstructor
public class AutorizacionEnvio {

    private final UsuarioActual usuario;

    /** Consultar el envío (detalle, seguimiento, historial o listado). */
    public void verificarLectura(Envio envio) {
        if (usuario.esAdmin() || esPropietario(envio) || estaAsignado(envio)) {
            return;
        }
        throw denegado("consultar");
    }

    /** Cancelar el envío: solo el dueño (o un ADMIN). */
    public void verificarCancelacion(Envio envio) {
        if (usuario.esAdmin() || esPropietario(envio)) {
            return;
        }
        throw denegado("cancelar");
    }

    /** Cambiar el estado: solo el repartidor asignado (o un ADMIN). */
    public void verificarCambioDeEstado(Envio envio) {
        if (usuario.esAdmin() || estaAsignado(envio)) {
            return;
        }
        throw denegado("cambiar el estado de");
    }

    /** Asignar repartidor: solo el dueño del envío (o un ADMIN). */
    public void verificarAsignacion(Envio envio) {
        if (usuario.esAdmin() || esPropietario(envio)) {
            return;
        }
        throw denegado("asignar un repartidor a");
    }

    /**
     * Filtro de repartidor efectivo para un listado.
     * <ul>
     *   <li>{@code ADMIN} y {@code CLIENTE} pueden filtrar por cualquier repartidor (el CLIENTE
     *       solo verá igualmente sus propios envíos).</li>
     *   <li>{@code REPARTIDOR} queda atado a su propio id: pedir la hoja de ruta de otro es 403.</li>
     * </ul>
     */
    public UUID repartidorIdParaListado(UUID solicitado) {
        if (usuario.esAdmin() || usuario.esCliente()) {
            return solicitado;
        }
        if (usuario.esRepartidor()) {
            UUID propio = usuario.repartidorId();
            if (propio == null) {
                throw new AccesoDenegadoException(
                    "El token del repartidor no incluye el claim repartidorId.");
            }
            if (solicitado != null && !solicitado.equals(propio)) {
                throw denegado("consultar los envíos de otro repartidor");
            }
            return propio;
        }
        throw denegado("listar");
    }

    /** UUID del usuario CLIENTE autenticado, obligatorio para filtrar por pertenencia. */
    public UUID usuarioIdDelCliente() {
        UUID usuarioId = usuario.usuarioId();
        if (usuarioId == null) {
            throw new AccesoDenegadoException(
                "El token no incluye un identificador de usuario (sub) válido.");
        }
        return usuarioId;
    }

    private boolean esPropietario(Envio envio) {
        if (!usuario.esCliente()) {
            return false;
        }
        return envio.getCliente().esDeUsuario(usuario.usuarioId());
    }

    private boolean estaAsignado(Envio envio) {
        if (!usuario.esRepartidor()) {
            return false;
        }
        UUID repartidorId = usuario.repartidorId();
        return repartidorId != null && repartidorId.equals(envio.getRepartidorId());
    }

    private AccesoDenegadoException denegado(String accion) {
        return new AccesoDenegadoException(
            "El usuario autenticado no tiene permiso para %s este envío.".formatted(accion));
    }
}
