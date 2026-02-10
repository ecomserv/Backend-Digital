package yea.ecomservapi.modules.quoting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import yea.ecomservapi.modules.quoting.domain.Quote;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, String> {

    List<Quote> findAllByOrderByCreatedAtDesc();

    @Query(value = "SELECT MAX(CAST(document_number AS integer)) FROM quotes WHERE document_number ~ '^\\d+$'", nativeQuery = true)
    Optional<Integer> findMaxDocumentNumber();

    boolean existsByDocumentNumber(String documentNumber);
}
