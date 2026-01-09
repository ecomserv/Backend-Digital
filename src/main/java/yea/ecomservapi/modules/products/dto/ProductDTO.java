package yea.ecomservapi.modules.products.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String code;
    private String description;
    private String unitMeasure;
    private BigDecimal referencePrice;
}
