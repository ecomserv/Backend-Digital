package yea.ecomservapi.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequest {
    @NotBlank(message = "Usuario es requerido")
    private String username;

    @NotBlank(message = "Contraseña es requerida")
    private String password;
}
