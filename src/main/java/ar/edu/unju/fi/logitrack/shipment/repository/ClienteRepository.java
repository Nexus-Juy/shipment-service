package ar.edu.unju.fi.logitrack.shipment.repository;

import ar.edu.unju.fi.logitrack.shipment.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    /** Útil para reutilizar el cliente si ya registró envíos antes (buscar-o-crear). */
    Optional<Cliente> findByEmailIgnoreCase(String email);
}
