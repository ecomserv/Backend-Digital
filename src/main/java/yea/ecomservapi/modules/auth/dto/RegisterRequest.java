package yea.ecomservapi.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Usuario es requerido")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 4, max = 100)
    private String password;

    @NotBlank(message = "Nombre es requerido")
    private String name;
}
