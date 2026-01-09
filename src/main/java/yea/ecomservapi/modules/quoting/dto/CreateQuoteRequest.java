package yea.ecomservapi.modules.quoting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import yea.ecomservapi.modules.quoting.domain.Currency;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuoteRequest {

    // Identificación
    private String documentNumber;

    private LocalDate documentDate;
    private LocalDate validUntil;
    private Currency currency;

    // Cliente
    private Long clientId;
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
    @NotEmpty(message = "Debe incluir al menos un item")
    @Valid
    private List<QuoteItemDTO> items;

    // Condiciones
    private String paymentCondition;
    private Integer validityDays;
    private String deliveryTime;
    private String warranty;
    private String notes;
}
