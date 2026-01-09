package yea.ecomservapi.modules.products.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yea.ecomservapi.modules.products.domain.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByCode(String code);

    List<Product> findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String code, String description);

    List<Product> findTop20ByOrderByUpdatedAtDesc();
}
