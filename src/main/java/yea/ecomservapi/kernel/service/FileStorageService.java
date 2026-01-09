package yea.ecomservapi.kernel.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.cotizaciones:cotizaciones}")
    private String cotizacionesFolder;

    private Path cotizacionesPath;

    private static final Pattern DOCUMENT_NUMBER_PATTERN = Pattern.compile("(\\d{5})\\.pdf");

    @PostConstruct
    public void init() {
        cotizacionesPath = Paths.get(cotizacionesFolder).toAbsolutePath().normalize();
        try {
            Files.createDirectories(cotizacionesPath);
            log.info("Carpeta de cotizaciones creada/verificada: {}", cotizacionesPath);
        } catch (IOException e) {
            log.error("No se pudo crear la carpeta de cotizaciones", e);
            throw new RuntimeException("Error al inicializar almacenamiento", e);
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveJson(Object data, String documentNumber) {
        try {
            String fileName = documentNumber + ".json";
            Path filePath = cotizacionesPath.resolve(fileName);
            objectMapper.writeValue(filePath.toFile(), data);
            log.info("JSON guardado: {}", filePath);
        } catch (IOException e) {
            log.error("Error al guardar JSON: {}", documentNumber, e);
        }
    }

    public <T> Optional<T> getJson(String documentNumber, Class<T> valueType) {
        try {
            String fileName = documentNumber + ".json";
            Path filePath = cotizacionesPath.resolve(fileName);
            if (Files.exists(filePath)) {
                return Optional.of(objectMapper.readValue(filePath.toFile(), valueType));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Error al leer JSON: {}", documentNumber, e);
            return Optional.empty();
        }
    }

    public String savePdf(byte[] pdfContent, String documentNumber) {
        try {
            String fileName = documentNumber + ".pdf";
            Path filePath = cotizacionesPath.resolve(fileName);
            Files.write(filePath, pdfContent);
            log.info("PDF guardado: {}", filePath);
            return fileName;
        } catch (IOException e) {
            log.error("Error al guardar PDF: {}", documentNumber, e);
            throw new RuntimeException("Error al guardar PDF", e);
        }
    }

    public Optional<byte[]> getPdf(String documentNumber) {
        try {
            String fileName = documentNumber + ".pdf";
            Path filePath = cotizacionesPath.resolve(fileName);
            if (Files.exists(filePath)) {
                return Optional.of(Files.readAllBytes(filePath));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Error al leer PDF: {}", documentNumber, e);
            return Optional.empty();
        }
    }

    public boolean deletePdf(String documentNumber) {
        try {
            String pdfName = documentNumber + ".pdf";
            String jsonName = documentNumber + ".json";
            Path pdfPath = cotizacionesPath.resolve(pdfName);
            Path jsonPath = cotizacionesPath.resolve(jsonName);

            boolean pdfDeleted = Files.deleteIfExists(pdfPath);
            boolean jsonDeleted = Files.deleteIfExists(jsonPath);

            return pdfDeleted || jsonDeleted;
        } catch (IOException e) {
            log.error("Error al eliminar archivos: {}", documentNumber, e);
            return false;
        }
    }

    public boolean existsPdf(String documentNumber) {
        String fileName = documentNumber + ".pdf";
        Path filePath = cotizacionesPath.resolve(fileName);
        return Files.exists(filePath);
    }

    public String generateNextDocumentNumber() {
        try (Stream<Path> files = Files.list(cotizacionesPath)) {
            int maxNumber = files
                    .map(path -> path.getFileName().toString())
                    .map(DOCUMENT_NUMBER_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> Integer.parseInt(matcher.group(1)))
                    .max(Integer::compareTo)
                    .orElse(0);

            return String.format("%05d", maxNumber + 1);
        } catch (IOException e) {
            log.error("Error al generar número de documento", e);
            return "00001";
        }
    }

    public Path getCotizacionesPath() {
        return cotizacionesPath;
    }

    public List<QuoteFileInfo> listAllQuotes() {
        try (Stream<Path> files = Files.list(cotizacionesPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                    .map(this::toQuoteFileInfo)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> b.documentNumber().compareTo(a.documentNumber()))
                    .toList();
        } catch (IOException e) {
            log.error("Error al listar cotizaciones", e);
            return List.of();
        }
    }

    public List<QuoteSummary> listAllQuotesWithSummary() {
        try (Stream<Path> files = Files.list(cotizacionesPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                    .map(this::toQuoteSummary)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                    .toList();
        } catch (IOException e) {
            log.error("Error al listar cotizaciones con resumen", e);
            return List.of();
        }
    }

    private Optional<QuoteSummary> toQuoteSummary(Path pdfPath) {
        try {
            String fileName = pdfPath.getFileName().toString();
            String documentNumber = fileName.replace(".pdf", "");

            BasicFileAttributes attrs = Files.readAttributes(pdfPath, BasicFileAttributes.class);
            long fileSize = attrs.size();
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

            // Try to read JSON for additional info
            String clientName = "";
            String currency = "PEN";
            double total = 0.0;
            int itemCount = 0;
            List<ItemDetail> itemDetails = new java.util.ArrayList<>();

            Path jsonPath = cotizacionesPath.resolve(documentNumber + ".json");
            if (Files.exists(jsonPath)) {
                try {
                    var jsonNode = objectMapper.readTree(jsonPath.toFile());

                    if (jsonNode.has("clientName")) {
                        clientName = jsonNode.get("clientName").asText("");
                    }
                    if (jsonNode.has("currency")) {
                        currency = jsonNode.get("currency").asText("PEN");
                    }
                    if (jsonNode.has("items") && jsonNode.get("items").isArray()) {
                        var items = jsonNode.get("items");
                        itemCount = items.size();

                        // Extract first 3 items with details
                        int count = 0;
                        for (var item : items) {
                            double qty = item.has("quantity") ? item.get("quantity").asDouble(0) : 0;
                            double price = item.has("unitPrice") ? item.get("unitPrice").asDouble(0) : 0;
                            double subtotal = qty * price;

                            if (count < 3 && item.has("description")) {
                                String desc = item.get("description").asText("");
                                // Truncate to 50 chars
                                if (desc.length() > 50) {
                                    desc = desc.substring(0, 47) + "...";
                                }
                                itemDetails.add(new ItemDetail(desc, qty, subtotal));
                            }
                            count++;

                            // Add to total
                            total += subtotal;
                        }
                        // Add IGV 18%
                        total = total * 1.18;
                    }
                } catch (IOException ex) {
                    log.warn("No se pudo leer JSON para {}: {}", documentNumber, ex.getMessage());
                }
            }

            return Optional.of(new QuoteSummary(
                    documentNumber,
                    clientName,
                    currency,
                    total,
                    itemCount,
                    itemDetails,
                    createdAt,
                    fileSize));
        } catch (IOException e) {
            log.error("Error al leer atributos de archivo: {}", pdfPath, e);
            return Optional.empty();
        }
    }

    private Optional<QuoteFileInfo> toQuoteFileInfo(Path path) {
        try {
            String fileName = path.getFileName().toString();
            Matcher matcher = DOCUMENT_NUMBER_PATTERN.matcher(fileName);
            if (!matcher.matches()) {
                return Optional.empty();
            }

            String documentNumber = fileName.replace(".pdf", "");
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            long fileSize = attrs.size();
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

            return Optional.of(new QuoteFileInfo(documentNumber, fileName, fileSize, createdAt));
        } catch (IOException e) {
            log.error("Error al leer atributos de archivo: {}", path, e);
            return Optional.empty();
        }
    }

    public record QuoteFileInfo(
            String documentNumber,
            String fileName,
            long fileSize,
            LocalDateTime createdAt) {
    }

    public record ItemDetail(
            String description,
            double quantity,
            double subtotal) {
    }

    public record QuoteSummary(
            String documentNumber,
            String clientName,
            String currency,
            double total,
            int itemCount,
            java.util.List<ItemDetail> itemDetails,
            LocalDateTime createdAt,
            long fileSize) {
    }
}
