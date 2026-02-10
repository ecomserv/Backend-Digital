package yea.ecomservapi.modules.reports.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class ReportStorageService {

    @Value("${app.storage.informes:informes}")
    private String informesFolder;

    private Path informesPath;

    private static final Pattern DOCUMENT_NUMBER_PATTERN = Pattern.compile("IT-(\\d{5})\\.pdf");

    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @PostConstruct
    public void init() {
        informesPath = Paths.get(informesFolder).toAbsolutePath().normalize();
        try {
            Files.createDirectories(informesPath);
            log.info("Carpeta de informes creada/verificada: {}", informesPath);
        } catch (IOException e) {
            log.error("No se pudo crear la carpeta de informes", e);
            throw new RuntimeException("Error al inicializar almacenamiento de informes", e);
        }
    }

    public void saveJson(Object data, String documentNumber) {
        try {
            String fileName = documentNumber + ".json.gz";
            Path filePath = informesPath.resolve(fileName);
            byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
            try (GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(filePath.toFile()))) {
                gzOut.write(jsonBytes);
            }
            log.info("Informe JSON GZIP guardado: {}", filePath);
        } catch (IOException e) {
            log.error("Error al guardar JSON de informe: {}", documentNumber, e);
        }
    }

    public <T> Optional<T> getJson(String documentNumber, Class<T> valueType) {
        try {
            Path gzPath = informesPath.resolve(documentNumber + ".json.gz");
            if (Files.exists(gzPath)) {
                try (GZIPInputStream gzIn = new GZIPInputStream(new FileInputStream(gzPath.toFile()))) {
                    return Optional.of(objectMapper.readValue(gzIn, valueType));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Error al leer JSON de informe: {}", documentNumber, e);
            return Optional.empty();
        }
    }

    public String savePdf(byte[] pdfContent, String documentNumber) {
        try {
            String fileName = documentNumber + ".pdf";
            Path filePath = informesPath.resolve(fileName);
            Files.write(filePath, pdfContent);
            log.info("Informe PDF guardado: {}", filePath);
            return fileName;
        } catch (IOException e) {
            log.error("Error al guardar PDF de informe: {}", documentNumber, e);
            throw new RuntimeException("Error al guardar PDF de informe", e);
        }
    }

    public Optional<byte[]> getPdf(String documentNumber) {
        try {
            String fileName = documentNumber + ".pdf";
            Path filePath = informesPath.resolve(fileName);
            if (Files.exists(filePath)) {
                return Optional.of(Files.readAllBytes(filePath));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Error al leer PDF de informe: {}", documentNumber, e);
            return Optional.empty();
        }
    }

    public boolean deletePdf(String documentNumber) {
        try {
            Path pdfPath = informesPath.resolve(documentNumber + ".pdf");
            Path jsonGzPath = informesPath.resolve(documentNumber + ".json.gz");

            boolean pdfDeleted = Files.deleteIfExists(pdfPath);
            boolean gzDeleted = Files.deleteIfExists(jsonGzPath);

            return pdfDeleted || gzDeleted;
        } catch (IOException e) {
            log.error("Error al eliminar archivos de informe: {}", documentNumber, e);
            return false;
        }
    }

    public boolean existsPdf(String documentNumber) {
        return Files.exists(informesPath.resolve(documentNumber + ".pdf"));
    }

    public String generateNextDocumentNumber() {
        try (Stream<Path> files = Files.list(informesPath)) {
            int maxNumber = files
                    .map(path -> path.getFileName().toString())
                    .map(DOCUMENT_NUMBER_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> Integer.parseInt(matcher.group(1)))
                    .max(Integer::compareTo)
                    .orElse(0);

            return String.format("IT-%05d", maxNumber + 1);
        } catch (IOException e) {
            log.error("Error al generar número de documento de informe", e);
            return "IT-00001";
        }
    }

    public List<ReportSummary> listAllReportsWithSummary() {
        try (Stream<Path> files = Files.list(informesPath)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".pdf"))
                    .map(this::toReportSummary)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                    .toList();
        } catch (IOException e) {
            log.error("Error al listar informes con resumen", e);
            return List.of();
        }
    }

    private Optional<ReportSummary> toReportSummary(Path pdfPath) {
        try {
            String fileName = pdfPath.getFileName().toString();
            String documentNumber = fileName.replace(".pdf", "");

            BasicFileAttributes attrs = Files.readAttributes(pdfPath, BasicFileAttributes.class);
            long fileSize = attrs.size();
            LocalDateTime createdAt = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

            // Try to read JSON for additional info
            String empresa = "";
            String tipoServicio = "";
            String marca = "";
            String modelo = "";
            String problemaReportado = "";
            String realizadoPor = "";

            com.fasterxml.jackson.databind.JsonNode jsonNode = readJsonNode(documentNumber);
            if (jsonNode != null) {
                try {
                    if (jsonNode.has("empresa")) empresa = jsonNode.get("empresa").asText("");
                    if (jsonNode.has("tipoServicio")) tipoServicio = jsonNode.get("tipoServicio").asText("");
                    if (jsonNode.has("marca")) marca = jsonNode.get("marca").asText("");
                    if (jsonNode.has("modelo")) modelo = jsonNode.get("modelo").asText("");
                    if (jsonNode.has("realizadoPor")) realizadoPor = jsonNode.get("realizadoPor").asText("");
                    if (jsonNode.has("problemaReportado")) {
                        problemaReportado = jsonNode.get("problemaReportado").asText("");
                        if (problemaReportado.length() > 80) {
                            problemaReportado = problemaReportado.substring(0, 77) + "...";
                        }
                    }
                } catch (Exception ex) {
                    log.warn("No se pudo parsear JSON para informe {}: {}", documentNumber, ex.getMessage());
                }
            }

            return Optional.of(new ReportSummary(
                    documentNumber,
                    empresa,
                    tipoServicio,
                    marca,
                    modelo,
                    problemaReportado,
                    realizadoPor,
                    createdAt,
                    fileSize));
        } catch (IOException e) {
            log.error("Error al leer atributos de archivo de informe: {}", pdfPath, e);
            return Optional.empty();
        }
    }

    private com.fasterxml.jackson.databind.JsonNode readJsonNode(String documentNumber) {
        try {
            Path gzPath = informesPath.resolve(documentNumber + ".json.gz");
            if (Files.exists(gzPath)) {
                try (GZIPInputStream gzIn = new GZIPInputStream(new FileInputStream(gzPath.toFile()))) {
                    return objectMapper.readTree(gzIn);
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo leer JSON para informe {}: {}", documentNumber, e.getMessage());
        }
        return null;
    }

    public Path getInformesPath() {
        return informesPath;
    }

    // ===== Records =====

    public record ReportSummary(
            String documentNumber,
            String empresa,
            String tipoServicio,
            String marca,
            String modelo,
            String problemaReportado,
            String realizadoPor,
            LocalDateTime createdAt,
            long fileSize) {
    }
}
