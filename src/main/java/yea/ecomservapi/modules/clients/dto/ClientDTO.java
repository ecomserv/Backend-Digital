package yea.ecomservapi.modules.clients.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {
    private Long id;
    private String name;
    private String ruc;
    private String address;
    private String phone;
    private String email;
    private String contactPerson;
}
