package yea.ecomservapi.modules.quoting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  /**
   * Send a quote email to the specified recipient using Gmail SMTP.
   * 
   * @param toEmail   Recipient email address
   * @param quoteData Quote data object
   * @param pdfBytes  PDF attachment bytes (optional)
   * @return true if email was sent successfully
   */
  public boolean sendQuoteEmail(String toEmail, yea.ecomservapi.modules.quoting.dto.QuoteDTO quoteData,
      byte[] pdfBytes) {
    try {
      jakarta.mail.internet.MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail, "ECOMSERV SAC");
      helper.setTo(toEmail);
      helper.setSubject("Cotización " + quoteData.getDocumentNumber() + " - ECOMSERV SAC");

      // Build HTML content
      String htmlContent = buildEmailHtml(quoteData);
      helper.setText(htmlContent, true);

      // Add PDF attachment if provided
      if (pdfBytes != null && pdfBytes.length > 0) {
        helper.addAttachment("Cotizacion-" + quoteData.getDocumentNumber() + ".pdf",
            new org.springframework.core.io.ByteArrayResource(pdfBytes));
      }

      log.info("Sending email to {} for quote {}", toEmail, quoteData.getDocumentNumber());
      javaMailSender.send(message);
      log.info("Email sent successfully to {} for quote {}", toEmail, quoteData.getDocumentNumber());
      return true;

    } catch (Exception e) {
      log.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
      return false;
    }
  }

  private String buildEmailHtml(yea.ecomservapi.modules.quoting.dto.QuoteDTO quoteData) {
    String clientName = quoteData.getClientName() != null ? quoteData.getClientName() : "Cliente";
    String quoteNumber = quoteData.getDocumentNumber();

    // Build items summary (max 2 items, then "etc.")
    String itemsSummary = buildItemsSummary(quoteData);

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            body { margin: 0; padding: 0; background-color: #f5f5f5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
            .container { max-width: 500px; margin: 20px auto; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
            .header { background: linear-gradient(135deg, #0f1d4a 0%, #1e3a8a 100%); padding: 20px; text-align: center; }
            .header h1 { color: #fff; margin: 0; font-size: 18px; font-weight: 600; letter-spacing: 0.3px; }
            .content { padding: 25px 20px; }
            .greeting { font-size: 14px; color: #333; margin: 0 0 15px 0; line-height: 1.5; }
            .greeting strong { color: #1e3a8a; }
            .items-text { font-size: 14px; color: #333; margin: 0 0 20px 0; line-height: 1.6; }
            .items-text strong { color: #1e3a8a; font-weight: 600; }
            .pdf-note { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px 15px; margin: 20px 0 0 0; border-radius: 0 8px 8px 0; font-size: 13px; color: #92400e; }
            .footer { background: #f8fafc; padding: 15px 20px; text-align: center; font-size: 11px; color: #64748b; border-top: 1px solid #e2e8f0; }
            .footer strong { color: #334155; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <h1>ECOMSERV SAC</h1>
            </div>
            <div class="content">
              <p class="greeting">Estimado(a) <strong>{{CLIENT_NAME}}</strong>,</p>
              <p class="items-text">Le adjuntamos su cotización <strong>{{QUOTE_NUMBER}}</strong> de: <strong>{{ITEMS_SUMMARY}}</strong>.</p>
              <div class="pdf-note">El detalle completo se encuentra en el archivo PDF adjunto.</div>
            </div>
            <div class="footer">
              <strong>ECOMSERV SAC</strong> | RUC: 20602689809 | Cel: 945464470
            </div>
          </div>
        </body>
        </html>
        """
        .replace("{{CLIENT_NAME}}", clientName)
        .replace("{{QUOTE_NUMBER}}", quoteNumber)
        .replace("{{ITEMS_SUMMARY}}", itemsSummary);
  }

  private String buildItemsSummary(yea.ecomservapi.modules.quoting.dto.QuoteDTO quoteData) {
    var items = quoteData.getItems();
    if (items == null || items.isEmpty()) {
      return "productos solicitados";
    }

    StringBuilder summary = new StringBuilder();
    int count = Math.min(items.size(), 2);

    for (int i = 0; i < count; i++) {
      var item = items.get(i);
      String description = item.getDescription();
      // Truncate long descriptions
      if (description != null && description.length() > 40) {
        description = description.substring(0, 37) + "...";
      }
      if (i > 0) {
        summary.append(", ");
      }
      summary.append(description != null ? description : "Producto");
    }

    if (items.size() > 2) {
      summary.append(", etc.");
    }

    return summary.toString();
  }
}
