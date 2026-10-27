package ar.edu.unju.fi.logitrack.shipment.repository;

import ar.edu.unju.fi.logitrack.shipment.entity.Envio;
import ar.edu.unju.fi.logitrack.shipment.entity.enums.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvioRepository extends JpaRepository<Envio, UUID> {

    boolean existsByCodigoSeguimiento(String codigoSeguimiento);

    Optional<Envio> findByCodigoSeguimiento(String codigoSeguimiento);

    List<Envio> findByEstadoOrderByCreatedAtDesc(EstadoEnvio estado);

    List<Envio> findByRepartidorIdOrderByCreatedAtDesc(UUID repartidorId);

    List<Envio> findByRepartidorIdAndEstadoOrderByCreatedAtDesc(UUID repartidorId, EstadoEnvio estado);

    /** Envíos de un cliente identificado por el usuario del token (S2-07). */
    List<Envio> findByClienteUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    List<Envio> findByClienteUsuarioIdAndEstadoOrderByCreatedAtDesc(UUID usuarioId, EstadoEnvio estado);

    boolean existsByRepartidorIdAndEstado(UUID repartidorId, EstadoEnvio estado);
}
