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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

        public Map<String, Object> getQuoteStats(LocalDate fromDate, LocalDate toDate) {
                List<Quote> allQuotes = quoteRepository.findAllByOrderByCreatedAtDesc();

                // Previous period comparison
                double previousPeriodPEN = 0, previousPeriodUSD = 0;
                int previousPeriodQuotes = 0;
                if (fromDate != null && toDate != null) {
                        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
                        LocalDate prevTo = fromDate.minusDays(1);
                        LocalDate prevFrom = prevTo.minusDays(daysBetween);
                        LocalDateTime prevFromDt = prevFrom.atStartOfDay();
                        LocalDateTime prevToDt = prevTo.plusDays(1).atStartOfDay();
                        for (Quote q : allQuotes) {
                                if (q.getCreatedAt() != null && !q.getCreatedAt().isBefore(prevFromDt) && q.getCreatedAt().isBefore(prevToDt)) {
                                        previousPeriodQuotes++;
                                        if ("USD".equals(q.getCurrency())) previousPeriodUSD += q.getTotal().doubleValue();
                                        else previousPeriodPEN += q.getTotal().doubleValue();
                                }
                        }
                }

                // Apply date filter
                List<Quote> filteredQuotes = allQuotes;
                if (fromDate != null && toDate != null) {
                        LocalDateTime fromDt = fromDate.atStartOfDay();
                        LocalDateTime toDt = toDate.plusDays(1).atStartOfDay();
                        filteredQuotes = allQuotes.stream()
                                .filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(fromDt) && q.getCreatedAt().isBefore(toDt))
                                .toList();
                }

                // Recent 5 quotes
                List<Map<String, Object>> recentQuotes = filteredQuotes.stream().limit(5).map(q -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("documentNumber", q.getDocumentNumber());
                        m.put("clientName", q.getClientName());
                        m.put("currency", q.getCurrency());
                        m.put("total", q.getTotal());
                        m.put("itemCount", q.getItemCount());
                        m.put("createdAt", q.getCreatedAt());
                        return m;
                }).toList();

                LocalDateTime now = LocalDateTime.now();
                int currentMonth = now.getMonthValue();
                int currentYear = now.getYear();
                LocalDateTime lastMonthStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime thisMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

                int quotesThisMonth = 0, quotesLastMonth = 0;
                double totalPEN = 0, totalUSD = 0;
                Map<String, int[]> clientCounts = new LinkedHashMap<>();
                Map<String, double[]> monthlyTotals = new LinkedHashMap<>();

                // Init monthly trend (last 6 months)
                for (int i = 5; i >= 0; i--) {
                        LocalDateTime m = now.minusMonths(i);
                        String key = String.format("%d-%02d", m.getYear(), m.getMonthValue());
                        monthlyTotals.put(key, new double[]{0, 0, 0}); // count, usd, pen
                }

                for (Quote q : filteredQuotes) {
                        LocalDateTime created = q.getCreatedAt();
                        if (created == null) continue;
                        double total = q.getTotal() != null ? q.getTotal().doubleValue() : 0;
                        boolean isUSD = "USD".equals(q.getCurrency());
                        if (isUSD) totalUSD += total; else totalPEN += total;

                        if (created.getMonthValue() == currentMonth && created.getYear() == currentYear) quotesThisMonth++;
                        if (!created.isBefore(lastMonthStart) && created.isBefore(thisMonthStart)) quotesLastMonth++;

                        String cn = q.getClientName() != null && !q.getClientName().isBlank() ? q.getClientName() : "Sin cliente";
                        clientCounts.computeIfAbsent(cn, k -> new int[]{0})[0]++;

                        String monthKey = String.format("%d-%02d", created.getYear(), created.getMonthValue());
                        double[] md = monthlyTotals.get(monthKey);
                        if (md != null) { md[0]++; md[1] += isUSD ? total : 0; md[2] += isUSD ? 0 : total; }
                }

                // Top 5 clients
                List<Map<String, Object>> topClients = clientCounts.entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
                        .limit(5)
                        .map(e -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("name", e.getKey()); m.put("count", e.getValue()[0]); return m; })
                        .toList();

                // Monthly trend
                List<Map<String, Object>> monthlyTrend = monthlyTotals.entrySet().stream().map(e -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("key", e.getKey());
                        m.put("count", (int) e.getValue()[0]);
                        m.put("totalUSD", Math.round(e.getValue()[1] * 100.0) / 100.0);
                        m.put("totalPEN", Math.round(e.getValue()[2] * 100.0) / 100.0);
                        return m;
                }).toList();

                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("quotesThisMonth", quotesThisMonth);
                stats.put("quotesLastMonth", quotesLastMonth);
                stats.put("totalPEN", Math.round(totalPEN * 100.0) / 100.0);
                stats.put("totalUSD", Math.round(totalUSD * 100.0) / 100.0);
                stats.put("totalQuotes", filteredQuotes.size());
                stats.put("topClients", topClients);
                stats.put("monthlyTrend", monthlyTrend);
                stats.put("previousPeriodPEN", Math.round(previousPeriodPEN * 100.0) / 100.0);
                stats.put("previousPeriodUSD", Math.round(previousPeriodUSD * 100.0) / 100.0);
                stats.put("previousPeriodQuotes", previousPeriodQuotes);
                stats.put("recentQuotes", recentQuotes);
                return stats;
        }
}
