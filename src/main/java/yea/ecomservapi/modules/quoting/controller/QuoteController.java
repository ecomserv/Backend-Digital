package yea.ecomservapi.modules.quoting.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yea.ecomservapi.kernel.service.PdfGeneratorService;
import yea.ecomservapi.modules.quoting.domain.Quote;
import yea.ecomservapi.modules.quoting.dto.CreateQuoteRequest;
import yea.ecomservapi.modules.quoting.dto.QuoteDTO;
import yea.ecomservapi.modules.quoting.dto.SendEmailRequest;
import yea.ecomservapi.modules.quoting.service.EmailService;
import yea.ecomservapi.modules.quoting.service.QuoteService;

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
    private final EmailService emailService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateAndSavePdf(@Valid @RequestBody CreateQuoteRequest request) {
        // Generar número de documento o usar el proporcionado
        String documentNumber;
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank()
                && !request.getDocumentNumber().equals("XXXXX")) {
            documentNumber = request.getDocumentNumber();
        } else {
            documentNumber = quoteService.generateNextDocumentNumber();
        }

        // Construir DTO con el número generado
        QuoteDTO quoteDTO = quoteService.buildQuoteDTO(request, documentNumber);

        // Generar PDF
        byte[] pdf = pdfGeneratorService.generateQuotePdf(quoteDTO);

        // Guardar en base de datos (solo JSON metadata)
        quoteService.saveQuote(request, documentNumber);

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
        // Regenerar PDF desde JSON en base de datos
        return quoteService.getQuoteData(documentNumber)
                .map(request -> {
                    QuoteDTO quoteDTO = quoteService.buildQuoteDTO(request, documentNumber);
                    byte[] pdf = pdfGeneratorService.generateQuotePdf(quoteDTO);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=" + documentNumber + ".pdf")
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(pdf);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{documentNumber}/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(@PathVariable String documentNumber) {
        boolean exists = quoteService.existsQuote(documentNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @DeleteMapping("/{documentNumber}")
    public ResponseEntity<Void> deletePdf(@PathVariable String documentNumber) {
        boolean deleted = quoteService.deleteQuote(documentNumber);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/next-number")
    public ResponseEntity<Map<String, String>> getNextDocumentNumber() {
        String nextNumber = quoteService.generateNextDocumentNumber();
        return ResponseEntity.ok(Map.of("documentNumber", nextNumber));
    }

    @GetMapping
    public ResponseEntity<List<QuoteSummaryDTO>> listAllQuotes() {
        List<Quote> quotes = quoteService.listAllQuotes();
        List<QuoteSummaryDTO> summaries = quotes.stream()
                .map(this::toSummaryDTO)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/summary")
    public ResponseEntity<List<QuoteSummaryDTO>> listAllQuotesWithSummary() {
        List<Quote> quotes = quoteService.listAllQuotes();
        List<QuoteSummaryDTO> summaries = quotes.stream()
                .map(this::toSummaryDTO)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/{documentNumber}/data")
    public ResponseEntity<CreateQuoteRequest> getQuoteData(@PathVariable String documentNumber) {
        return quoteService.getQuoteData(documentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/send-email")
    public ResponseEntity<Map<String, Object>> sendQuoteEmail(@Valid @RequestBody SendEmailRequest request) {
        // Cargar datos de la base de datos
        CreateQuoteRequest quoteRequest = quoteService.getQuoteData(request.getDocumentNumber()).orElse(null);

        if (quoteRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No se encontraron los datos de la cotización"));
        }

        // Construir QuoteDTO con totales calculados
        QuoteDTO quoteData = quoteService.buildQuoteDTO(quoteRequest, request.getDocumentNumber());

        // Regenerar PDF si se solicita adjuntar
        byte[] pdfBytes = null;
        if (request.isAttachPdf()) {
            pdfBytes = pdfGeneratorService.generateQuotePdf(quoteData);
        }

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

    private QuoteSummaryDTO toSummaryDTO(Quote quote) {
        return new QuoteSummaryDTO(
                quote.getDocumentNumber(),
                quote.getClientName(),
                quote.getCurrency(),
                quote.getTotal(),
                quote.getItemCount(),
                quote.getCreatedAt());
    }

    // Inner DTO for summary response
    public record QuoteSummaryDTO(
            String documentNumber,
            String clientName,
            String currency,
            java.math.BigDecimal total,
            Integer itemCount,
            java.time.LocalDateTime createdAt) {
    }
}
