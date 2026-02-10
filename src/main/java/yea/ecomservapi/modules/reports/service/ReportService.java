package yea.ecomservapi.modules.reports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yea.ecomservapi.modules.reports.dto.CreateReportRequest;
import yea.ecomservapi.modules.reports.dto.ReportDTO;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    public ReportDTO buildReportDTO(CreateReportRequest request, String documentNumber) {
        return ReportDTO.builder()
                .documentNumber(documentNumber)
                .documentDate(request.getDocumentDate() != null ? request.getDocumentDate() : LocalDate.now())
                .tipoHardware(request.getTipoHardware() != null ? request.getTipoHardware() : "")
                .tipoServicio(request.getTipoServicio() != null ? request.getTipoServicio() : "")
                .marca(request.getMarca() != null ? request.getMarca() : "")
                .modelo(request.getModelo() != null ? request.getModelo() : "")
                .serialNumber(request.getSerialNumber() != null ? request.getSerialNumber() : "")
                .realizadoPor(request.getRealizadoPor() != null ? request.getRealizadoPor() : "")
                .empresa(request.getEmpresa() != null ? request.getEmpresa() : "")
                .area(request.getArea() != null ? request.getArea() : "")
                .sede(request.getSede() != null ? request.getSede() : "")
                .numeroOrden(request.getNumeroOrden() != null ? request.getNumeroOrden() : "")
                .problemaReportado(request.getProblemaReportado() != null ? request.getProblemaReportado() : "")
                .pruebasRealizadas(request.getPruebasRealizadas() != null ? request.getPruebasRealizadas() : List.of())
                .conclusiones(request.getConclusiones() != null ? request.getConclusiones() : List.of())
                .recomendaciones(request.getRecomendaciones() != null ? request.getRecomendaciones() : List.of())
                .observaciones(request.getObservaciones() != null ? request.getObservaciones() : "")
                .build();
    }
}
