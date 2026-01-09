package yea.ecomservapi.modules.quoting.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(name = "json_data", columnDefinition = "TEXT", nullable = false)
    private String jsonData;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "total", precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "item_count")
    private Integer itemCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
