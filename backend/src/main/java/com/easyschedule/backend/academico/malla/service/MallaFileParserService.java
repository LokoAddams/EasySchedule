package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaImportRequest;
import com.easyschedule.backend.academico.malla.dto.MateriaImportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MallaFileParserService {

    private static final Logger logger = LoggerFactory.getLogger(MallaFileParserService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public MallaImportRequest parseFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".json")) {
            return parseJson(file);
        } else if (lowerFilename.endsWith(".csv")) {
            return parseCsv(file);
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado. Use CSV o JSON");
        }
    }

    private MallaImportRequest parseJson(MultipartFile file) {
        try {
            MallaJsonWrapper wrapper = objectMapper.readValue(file.getInputStream(), MallaJsonWrapper.class);
            return new MallaImportRequest(
                wrapper.nombre(),
                wrapper.version(),
                wrapper.carreraId(),
                wrapper.materias()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al parsear JSON: " + e.getMessage());
        }
    }

    private MallaImportRequest parseCsv(MultipartFile file) {
        List<MateriaImportRequest> materias = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        logger.info("Parsing CSV file: {}, size={} bytes", file.getOriginalFilename(), file.getSize());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                // Eliminar BOM si existe
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                    logger.debug("BOM detectado y removido de la primera línea");
                }

                // Log primeras líneas para debug
                if (lineNumber <= 3) {
                    logger.debug("Línea {}: {}", lineNumber, line);
                }

                String[] parts = line.split(",", -1);

                if (lineNumber == 1) {
                    headers = parts;
                    validateCsvHeaders(headers);
                    continue;
                }

                if (parts.length < 4) {
                    errors.add("Línea " + lineNumber + ": Se requieren al menos 4 columnas");
                    continue;
                }

                try {
                    String codigo = parts[0].trim();
                    String nombre = parts[1].trim();
                    int semestre = Integer.parseInt(parts[2].trim());
                    int creditos = parts[3].trim().isEmpty() ? 0 : Integer.parseInt(parts[3].trim());

                    List<String> prerequisitos = new ArrayList<>();
                    if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                        String[] prereqs = parts[4].split(";");
                        for (String p : prereqs) {
                            if (!p.trim().isEmpty()) {
                                prerequisitos.add(p.trim());
                            }
                        }
                    }

                    materias.add(new MateriaImportRequest(codigo, nombre, semestre, creditos, prerequisitos));
                } catch (NumberFormatException e) {
                    errors.add("Línea " + lineNumber + ": Error en formato numérico");
                }
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Errores en CSV:\n" + String.join("\n", errors));
            }

            if (materias.isEmpty()) {
                throw new IllegalArgumentException("El archivo CSV no contiene materias válidas");
            }

            return new MallaImportRequest("Malla Importada", "1.0", null, materias);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al leer archivo CSV: " + e.getMessage());
        }
    }

    private void validateCsvHeaders(String[] headers) {
        if (headers.length < 4) {
            throw new IllegalArgumentException("El CSV debe tener al menos: codigo,nombre,semestre,creditos");
        }
    }

    public record MallaJsonWrapper(
        String nombre,
        String version,
        Long carreraId,
        List<MateriaImportRequest> materias
    ) {}
}
