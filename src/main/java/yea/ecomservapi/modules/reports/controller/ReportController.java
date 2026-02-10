package yea.ecomservapi.modules.reports.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yea.ecomservapi.kernel.service.PdfGeneratorService;
import yea.ecomservapi.modules.quoting.service.EmailService;
import yea.ecomservapi.modules.reports.dto.CreateReportRequest;
import yea.ecomservapi.modules.reports.dto.ReportDTO;
import yea.ecomservapi.modules.reports.dto.SendReportEmailRequest;
import yea.ecomservapi.modules.reports.service.ReportService;
import yea.ecomservapi.modules.reports.service.ReportStorageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Informes Técnicos", description = "Gestión de informes técnicos PDF")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;
    private final PdfGeneratorService pdfGeneratorService;
    private final ReportStorageService reportStorageService;
    private final EmailService emailService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateAndSavePdf(@RequestBody CreateReportRequest request) {
        String documentNumber;
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()
                && !request.getDocumentNumber().equals("IT-XXXXX")) {
            documentNumber = request.getDocumentNumber();
        } else {
            documentNumber = reportStorageService.generateNextDocumentNumber();
        }

        ReportDTO reportDTO = reportService.buildReportDTO(request, documentNumber);
        byte[] pdf = pdfGeneratorService.generateReportPdf(reportDTO);

        reportStorageService.savePdf(pdf, documentNumber);
        reportStorageService.saveJson(request, documentNumber);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + documentNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/preview")
    public ResponseEntity<byte[]> previewPdf(@RequestBody CreateReportRequest request) {
        String tempNumber = request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()
                ? request.getDocumentNumber()
                : "IT-PREVIEW";
        ReportDTO reportDTO = reportService.buildReportDTO(request, tempNumber);
        byte[] pdf = pdfGeneratorService.generateReportPdf(reportDTO);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{documentNumber}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String documentNumber) {
        return reportStorageService.getPdf(documentNumber)
                .map(pdf -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=" + documentNumber + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{documentNumber}/data")
    public ResponseEntity<CreateReportRequest> getReportData(@PathVariable String documentNumber) {
        return reportStorageService.getJson(documentNumber, CreateReportRequest.class)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{documentNumber}/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(@PathVariable String documentNumber) {
        boolean exists = reportStorageService.existsPdf(documentNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @DeleteMapping("/{documentNumber}")
    public ResponseEntity<Void> deletePdf(@PathVariable String documentNumber) {
        boolean deleted = reportStorageService.deletePdf(documentNumber);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/next-number")
    public ResponseEntity<Map<String, String>> getNextDocumentNumber() {
        String nextNumber = reportStorageService.generateNextDocumentNumber();
        return ResponseEntity.ok(Map.of("documentNumber", nextNumber));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<ReportStorageService.ReportSummary>> listAllReportsWithSummary() {
        return ResponseEntity.ok(reportStorageService.listAllReportsWithSummary());
    }

    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendReportEmail(@RequestBody SendReportEmailRequest request) {
        byte[] pdfBytes = null;
        if (request.isAttachPdf()) {
            pdfBytes = reportStorageService.getPdf(request.getDocumentNumber()).orElse(null);
        }
        boolean sent = emailService.sendReportEmail(
                request.getToEmail(),
                request.getEmpresa(),
                request.getDocumentNumber(),
                pdfBytes
        );
        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Email enviado correctamente"));
        }
        return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error al enviar email"));
    }
}
