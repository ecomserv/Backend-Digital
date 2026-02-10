package yea.ecomservapi.modules.reports.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendReportEmailRequest {

    @NotBlank(message = "El correo electrónico es requerido")
    @Email(message = "El correo electrónico no es válido")
    private String toEmail;

    @NotBlank(message = "El número de documento es requerido")
    private String documentNumber;

    private String empresa;

    private boolean attachPdf = true;
}
