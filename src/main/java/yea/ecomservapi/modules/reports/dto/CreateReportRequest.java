package yea.ecomservapi.modules.reports.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {

    // Identificación
    private String documentNumber;
    private LocalDate documentDate;

    // I. Datos Generales
    private String tipoHardware;
    private String tipoServicio;
    private String marca;
    private String modelo;
    private String serialNumber;
    private String realizadoPor;
    private String empresa;
    private String area;
    private String sede;
    private String numeroOrden;

    // Cliente vinculado (opcional, para autocompletar)
    private Long clientId;

    // II. Diagnóstico
    private String problemaReportado;
    private List<String> pruebasRealizadas;

    // III. Resultados
    private List<String> conclusiones;
    private List<String> recomendaciones;

    // IV. Observaciones
    private String observaciones;
}
