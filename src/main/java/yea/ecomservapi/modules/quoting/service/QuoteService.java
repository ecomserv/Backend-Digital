package yea.ecomservapi.modules.quoting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yea.ecomservapi.modules.quoting.domain.Currency;
import yea.ecomservapi.modules.quoting.domain.Quote;
import yea.ecomservapi.modules.quoting.dto.CreateQuoteRequest;
import yea.ecomservapi.modules.quoting.dto.QuoteDTO;
import yea.ecomservapi.modules.quoting.dto.QuoteItemDTO;
import yea.ecomservapi.modules.quoting.repository.QuoteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

        private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

        private final QuoteRepository quoteRepository;
        private final ObjectMapper objectMapper;

        public QuoteDTO buildQuoteDTO(CreateQuoteRequest request, String documentNumber) {
                List<QuoteItemDTO> items = request.getItems().stream()
                                .map(item -> {
                                        BigDecimal subtotal = item.getQuantity()
                                                        .multiply(item.getUnitPrice())
                                                        .setScale(2, RoundingMode.HALF_UP);
                                        return QuoteItemDTO.builder()
                                                        .code(item.getCode())
                                                        .description(item.getDescription())
                                                        .unitMeasure(item.getUnitMeasure() != null
                                                                        ? item.getUnitMeasure()
                                                                        : "UND")
                                                        .quantity(item.getQuantity())
                                                        .unitPrice(item.getUnitPrice())
                                                        .subtotal(subtotal)
                                                        .build();
                                })
                                .collect(Collectors.toList());

                BigDecimal subtotal = items.stream()
                                .map(QuoteItemDTO::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal igv = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = subtotal.add(igv);

                return QuoteDTO.builder()
                                .documentNumber(documentNumber)
                                .documentDate(request.getDocumentDate() != null ? request.getDocumentDate()
                                                : LocalDate.now())
                                .validUntil(request.getValidUntil() != null ? request.getValidUntil()
                                                : LocalDate.now().plusDays(4))
                                .currency(request.getCurrency() != null ? request.getCurrency() : Currency.PEN)
                                .clientName(request.getClientName())
                                .clientRuc(request.getClientRuc())
                                .clientAddress(request.getClientAddress())
                                .clientPhone(request.getClientPhone())
                                .clientEmail(request.getClientEmail())
                                .clientReference(request.getClientReference())
                                .clientMobile(request.getClientMobile())
                                .vendedor(request.getVendedor())
                                .atte(request.getAtte())
                                .items(items)
                                .subtotal(subtotal)
                                .igv(igv)
                                .total(total)
                                .paymentCondition(request.getPaymentCondition() != null ? request.getPaymentCondition()
                                                : "CONTADO")
                                .validityDays(request.getValidityDays() != null ? request.getValidityDays() : 4)
                                .deliveryTime(request.getDeliveryTime() != null ? request.getDeliveryTime()
                                                : "SEGUN STOCK INMEDIATO")
                                .warranty(request.getWarranty() != null ? request.getWarranty() : "12 MESES")
                                .notes(request.getNotes())
                                .build();
        }

        // ========== Database Operations ==========

        @Transactional
        public Quote saveQuote(CreateQuoteRequest request, String documentNumber) {
                try {
                        String jsonData = objectMapper.writeValueAsString(request);

                        // Calculate total for summary
                        BigDecimal total = calculateTotal(request);

                        // Get first item description for display
                        String firstItemDesc = null;
                        if (request.getItems() != null && !request.getItems().isEmpty()) {
                                firstItemDesc = request.getItems().get(0).getDescription();
                                if (firstItemDesc != null && firstItemDesc.length() > 50) {
                                        firstItemDesc = firstItemDesc.substring(0, 47) + "...";
                                }
                        }

                        Quote quote = Quote.builder()
                                        .documentNumber(documentNumber)
                                        .jsonData(jsonData)
                                        .clientName(request.getClientName())
                                        .currency(request.getCurrency() != null ? request.getCurrency().name() : "PEN")
                                        .total(total)
                                        .itemCount(request.getItems() != null ? request.getItems().size() : 0)
                                        .firstItemDescription(firstItemDesc)
                                        .build();

                        return quoteRepository.save(quote);
                } catch (JsonProcessingException e) {
                        log.error("Error serializing quote to JSON", e);
                        throw new RuntimeException("Error saving quote", e);
                }
        }

        public Optional<CreateQuoteRequest> getQuoteData(String documentNumber) {
                return quoteRepository.findById(documentNumber)
                                .map(quote -> {
                                        try {
                                                return objectMapper.readValue(quote.getJsonData(),
                                                                CreateQuoteRequest.class);
                                        } catch (JsonProcessingException e) {
                                                log.error("Error deserializing quote JSON", e);
                                                return null;
                                        }
                                });
        }

        public Optional<Quote> getQuote(String documentNumber) {
                return quoteRepository.findById(documentNumber);
        }

        @Transactional
        public boolean deleteQuote(String documentNumber) {
                if (quoteRepository.existsByDocumentNumber(documentNumber)) {
                        quoteRepository.deleteById(documentNumber);
                        return true;
                }
                return false;
        }

        public List<Quote> listAllQuotes() {
                return quoteRepository.findAllByOrderByCreatedAtDesc();
        }

        public boolean existsQuote(String documentNumber) {
                return quoteRepository.existsByDocumentNumber(documentNumber);
        }

        public String generateNextDocumentNumber() {
                int maxNumber = quoteRepository.findMaxDocumentNumber().orElse(0);
                return String.format("%05d", maxNumber + 1);
        }

        private BigDecimal calculateTotal(CreateQuoteRequest request) {
                if (request.getItems() == null || request.getItems().isEmpty()) {
                        return BigDecimal.ZERO;
                }

                BigDecimal subtotal = request.getItems().stream()
                                .map(item -> item.getQuantity().multiply(item.getUnitPrice()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal igv = subtotal.multiply(IGV_RATE);
                return subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        }
}
