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

    @Query("SELECT MAX(CAST(q.documentNumber AS integer)) FROM Quote q WHERE q.documentNumber NOT LIKE '%PREVIEW%'")
    Optional<Integer> findMaxDocumentNumber();

    boolean existsByDocumentNumber(String documentNumber);
}
