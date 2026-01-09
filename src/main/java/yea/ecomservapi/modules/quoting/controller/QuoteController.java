package yea.ecomservapi.modules.quoting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yea.ecomservapi.kernel.service.FileStorageService;
import yea.ecomservapi.kernel.service.PdfGeneratorService;
import yea.ecomservapi.modules.quoting.dto.CreateQuoteRequest;
import yea.ecomservapi.modules.quoting.dto.QuoteDTO;
import yea.ecomservapi.modules.quoting.service.QuoteService;
import yea.ecomservapi.modules.quoting.service.EmailService;
import yea.ecomservapi.modules.quoting.dto.SendEmailRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Tag(name = "Cotizaciones", description = "Gestión de cotizaciones PDF")
@SecurityRequirement(name = "Bearer Authentication")
public class QuoteController {

    private final QuoteService quoteService;
    private final PdfGeneratorService pdfGeneratorService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateAndSavePdf(@Valid @RequestBody CreateQuoteRequest request) {
        // Generar número de documento o usar el proporcionado
        String documentNumber;
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()
                && !request.getDocumentNumber().equals("CES-XXXXX")) {
            documentNumber = request.getDocumentNumber();
        } else {
            documentNumber = fileStorageService.generateNextDocumentNumber();
        }

        // Construir DTO con el número generado
        QuoteDTO quoteDTO = quoteService.buildQuoteDTO(request, documentNumber);

        // Generar PDF
        byte[] pdf = pdfGeneratorService.generateQuotePdf(quoteDTO);

        // Guardar PDF en carpeta local
        fileStorageService.savePdf(pdf, documentNumber);

        // Guardar datos JSON para futura edición
        fileStorageService.saveJson(request, documentNumber);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + documentNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/preview")
    public ResponseEntity<byte[]> previewPdf(@Valid @RequestBody CreateQuoteRequest request) {
        // Solo genera el PDF sin guardarlo (para vista previa)
        String tempNumber = "PREVIEW-" + System.currentTimeMillis();
        QuoteDTO quoteDTO = quoteService.buildQuoteDTO(request, tempNumber);
        byte[] pdf = pdfGeneratorService.generateQuotePdf(quoteDTO);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{documentNumber}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String documentNumber) {
        return fileStorageService.getPdf(documentNumber)
                .map(pdf -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=" + documentNumber + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{documentNumber}/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(@PathVariable String documentNumber) {
        boolean exists = fileStorageService.existsPdf(documentNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @DeleteMapping("/{documentNumber}")
    public ResponseEntity<Void> deletePdf(@PathVariable String documentNumber) {
        boolean deleted = fileStorageService.deletePdf(documentNumber);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/next-number")
    public ResponseEntity<Map<String, String>> getNextDocumentNumber() {
        String nextNumber = fileStorageService.generateNextDocumentNumber();
        return ResponseEntity.ok(Map.of("documentNumber", nextNumber));
    }

    @GetMapping
    public ResponseEntity<List<FileStorageService.QuoteFileInfo>> listAllQuotes() {
        return ResponseEntity.ok(fileStorageService.listAllQuotes());
    }

    @GetMapping("/summary")
    public ResponseEntity<List<FileStorageService.QuoteSummary>> listAllQuotesWithSummary() {
        return ResponseEntity.ok(fileStorageService.listAllQuotesWithSummary());
    }

    @GetMapping("/{documentNumber}/data")
    public ResponseEntity<CreateQuoteRequest> getQuoteData(@PathVariable String documentNumber) {
        return fileStorageService.getJson(documentNumber, CreateQuoteRequest.class)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendQuoteEmail(@Valid @RequestBody SendEmailRequest request) {
        byte[] pdfBytes = null;

        // Cargar PDF si se solicita adjuntar
        if (request.isAttachPdf()) {
            pdfBytes = fileStorageService.getPdf(request.getDocumentNumber()).orElse(null);
            if (pdfBytes == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message",
                                "No se encontró el PDF para la cotización especificada"));
            }
        }

        // Cargar datos como CreateQuoteRequest y convertir a QuoteDTO con totales
        CreateQuoteRequest quoteRequest = fileStorageService.getJson(request.getDocumentNumber(), CreateQuoteRequest.class).orElse(null);

        if (quoteRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No se encontraron los datos de la cotización"));
        }

        // Construir QuoteDTO con totales calculados
        QuoteDTO quoteData = quoteService.buildQuoteDTO(quoteRequest, request.getDocumentNumber());

        boolean sent = emailService.sendQuoteEmail(
                request.getToEmail(),
                quoteData,
                pdfBytes);

        if (sent) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Correo enviado correctamente"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Error al enviar el correo"));
        }
    }
}
