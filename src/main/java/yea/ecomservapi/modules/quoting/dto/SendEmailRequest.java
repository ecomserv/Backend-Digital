package yea.ecomservapi.modules.quoting.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for sending a quote via email.
 */
@Data
public class SendEmailRequest {

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El correo electrónico no es válido")
    private String toEmail;

    @NotBlank(message = "El número de documento es requerido")
    private String documentNumber;

    private String clientName;

    // Optional: if true, attach the PDF to the email
    private boolean attachPdf = true;
}
