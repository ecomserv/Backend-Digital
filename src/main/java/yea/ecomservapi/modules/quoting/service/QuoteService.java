package yea.ecomservapi.modules.quoting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yea.ecomservapi.modules.quoting.domain.Currency;
import yea.ecomservapi.modules.quoting.dto.CreateQuoteRequest;
import yea.ecomservapi.modules.quoting.dto.QuoteDTO;
import yea.ecomservapi.modules.quoting.dto.QuoteItemDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    public QuoteDTO buildQuoteDTO(CreateQuoteRequest request, String documentNumber) {
        // Calcular totales
        List<QuoteItemDTO> items = request.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getQuantity()
                            .multiply(item.getUnitPrice())
                            .setScale(2, RoundingMode.HALF_UP);
                    return QuoteItemDTO.builder()
                            .code(item.getCode())
                            .description(item.getDescription())
                            .unitMeasure(item.getUnitMeasure() != null ? item.getUnitMeasure() : "UND")
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
                .documentDate(request.getDocumentDate() != null ? request.getDocumentDate() : LocalDate.now())
                .validUntil(request.getValidUntil() != null ? request.getValidUntil() : LocalDate.now().plusDays(4))
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
                .paymentCondition(request.getPaymentCondition() != null ? request.getPaymentCondition() : "CONTADO")
                .validityDays(request.getValidityDays() != null ? request.getValidityDays() : 4)
                .deliveryTime(request.getDeliveryTime() != null ? request.getDeliveryTime() : "SEGUN STOCK INMEDIATO")
                .warranty(request.getWarranty() != null ? request.getWarranty() : "12 MESES")
                .notes(request.getNotes())
                .build();
    }
}
