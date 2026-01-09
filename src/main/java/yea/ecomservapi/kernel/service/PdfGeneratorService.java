package yea.ecomservapi.kernel.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import yea.ecomservapi.modules.quoting.dto.QuoteDTO;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGeneratorService {

    private final TemplateEngine templateEngine;

    private static final DecimalFormat DECIMAL_FORMAT;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("3.80");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "PE"));
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        DECIMAL_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public byte[] generateQuotePdf(QuoteDTO quote) {
        try {
            String html = generateHtml(quote);
            return convertHtmlToPdf(html);
        } catch (Exception e) {
            log.error("Error generating PDF for quote: {}", quote.getDocumentNumber(), e);
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private String generateHtml(QuoteDTO quote) {
        Context context = new Context();
        context.setVariable("quote", quote);
        context.setVariable("dateFormatter", DATE_FORMATTER);

        // Formatear valores para mostrar
        boolean isUSD = "USD".equals(quote.getCurrency() != null ? quote.getCurrency().name() : "PEN");
        String symbol = isUSD ? "US$" : "S/.";

        context.setVariable("symbol", symbol);
        context.setVariable("subtotalFormatted", symbol + " " + formatNumber(quote.getSubtotal()));
        context.setVariable("igvFormatted", symbol + " " + formatNumber(quote.getIgv()));
        context.setVariable("totalFormatted", symbol + " " + formatNumber(quote.getTotal()));

        // Total en soles
        BigDecimal totalPEN = isUSD
                ? quote.getTotal().multiply(EXCHANGE_RATE)
                : quote.getTotal();
        context.setVariable("totalPEN", "S/. " + formatNumber(totalPEN));

        // Información de empresa (hardcoded como en el frontend)
        context.setVariable("companyRuc", "20602689809");
        context.setVariable("companyAddress", "Urb. Faucett Mz E Lte 8 - Callao");
        context.setVariable("companyPhone", "945464470");
        context.setVariable("companyWeb", "www.ecomserv.com");

        // Info bancaria
        context.setVariable("bankTitular", "ECOMSERV SAC");
        context.setVariable("bankRuc", "20602689809");
        context.setVariable("bankAccount", "1912486011021");
        context.setVariable("bankCCI", "002-19100248601102152");

        // Imágenes embebidas en base64
        context.setVariable("logoBase64", loadImageAsBase64("static/logo-ecomserv.png"));
        context.setVariable("firmaBase64", loadImageAsBase64("static/firma_digital.png"));
        context.setVariable("footerBase64", loadImageAsBase64("static/footer-brands.png"));

        return templateEngine.process("quote-template", context);
    }

    private String loadImageAsBase64(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream is = resource.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar imagen: {}", resourcePath, e);
            return "";
        }
    }

    private byte[] convertHtmlToPdf(String html) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private String formatNumber(BigDecimal number) {
        if (number == null)
            return "0.00";
        return DECIMAL_FORMAT.format(number);
    }
}
