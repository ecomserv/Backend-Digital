package yea.ecomservapi.modules.quoting.dto;

import lombok.*;
import yea.ecomservapi.modules.quoting.domain.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteDTO {
    private String documentNumber;
    private LocalDate documentDate;
    private LocalDate validUntil;
    private Currency currency;

    // Datos del cliente
    private String clientName;
    private String clientRuc;
    private String clientAddress;
    private String clientPhone;
    private String clientEmail;
    private String clientReference;
    private String clientMobile;
    private String vendedor;
    private String atte;

    // Items
    private List<QuoteItemDTO> items;

    // Totales
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    // Condiciones
    private String paymentCondition;
    private Integer validityDays;
    private String deliveryTime;
    private String warranty;
    private String notes;
}
