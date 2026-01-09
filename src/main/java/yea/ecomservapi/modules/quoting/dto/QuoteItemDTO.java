package yea.ecomservapi.modules.quoting.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteItemDTO {
    private Long id;
    private String code;
    private String description;
    private String unitMeasure;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
