package yea.ecomservapi.modules.clients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yea.ecomservapi.modules.clients.domain.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByRuc(String ruc);

    List<Client> findByNameContainingIgnoreCaseOrRucContaining(String name, String ruc);

    List<Client> findTop10ByOrderByUpdatedAtDesc();
}
