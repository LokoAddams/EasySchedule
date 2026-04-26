package com.easyschedule.backend.academico.horario.service;

import com.easyschedule.backend.academico.carrera.model.Carrera;
import com.easyschedule.backend.academico.carrera.repository.CarreraRepository;
import com.easyschedule.backend.academico.horario.dto.HorarioActualResponse;
import com.easyschedule.backend.academico.horario.dto.HorarioClaseResponse;
import com.easyschedule.backend.academico.oferta_materia.repository.OfertaMateriaRepository;
import com.easyschedule.backend.academico.universidad.model.Universidad;
import com.easyschedule.backend.academico.universidad.repository.UniversidadRepository;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HorarioRecomendadoService {

    private final EstudianteRepository estudianteRepository;
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final OfertaMateriaRepository ofertaMateriaRepository;
    private final ObjectMapper objectMapper;

    public HorarioRecomendadoService(
        EstudianteRepository estudianteRepository,
        UniversidadRepository universidadRepository,
        CarreraRepository carreraRepository,
        OfertaMateriaRepository ofertaMateriaRepository,
        ObjectMapper objectMapper
    ) {
        this.estudianteRepository = estudianteRepository;
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.ofertaMateriaRepository = ofertaMateriaRepository;
        this.objectMapper = objectMapper;
    }

    public HorarioActualResponse getHorarioActualByUserId(Long userId) {
        Estudiante estudiante = estudianteRepository.findById(userId).orElse(null);

        if (estudiante == null || estudiante.getMalla() == null || estudiante.getSemestreActual() == null) {
            return new HorarioActualResponse(null, null, null, null, null, List.of());
        }

        if (estudiante.getMalla().getId() == null) {
            return new HorarioActualResponse(null, null, null, null, estudiante.getSemestreActual(), List.of());
        }

        Universidad universidad = estudiante.getUniversidadId() == null
            ? null
            : universidadRepository.findByIdAndActiveTrue(estudiante.getUniversidadId()).orElse(null);
        Carrera carrera = estudiante.getCarreraId() == null
            ? null
            : carreraRepository.findByIdAndActiveTrue(estudiante.getCarreraId()).orElse(null);

        List<OfertaMateriaRepository.HorarioOfertaRow> rows = ofertaMateriaRepository.findHorarioActualRows(
            userId,
            estudiante.getMalla().getId(),
            estudiante.getSemestreActual()
        );

        List<HorarioClaseResponse> clases = new ArrayList<>();
        String semestreOferta = null;
        for (OfertaMateriaRepository.HorarioOfertaRow row : rows) {
            if (semestreOferta == null) {
                semestreOferta = row.getSemestre();
            }
            clases.addAll(parseHorario(row));
        }

        String mallaLabel = estudiante.getMalla().getNombre();
        if (mallaLabel == null || mallaLabel.isBlank()) {
            if (estudiante.getMalla().getVersion() == null) {
                mallaLabel = null;
            } else {
                mallaLabel = "Malla " + estudiante.getMalla().getVersion();
            }
        }

        return new HorarioActualResponse(
            universidad == null ? null : universidad.getNombre(),
            carrera == null ? null : carrera.getNombre(),
            mallaLabel,
            semestreOferta,
            estudiante.getSemestreActual(),
            clases
        );
    }

    public byte[] buildHorarioActualCsv(Long userId) {
        HorarioActualResponse horario = getHorarioActualByUserId(userId);
        String csv = toCsv(horario);
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] buildHorarioActualPdf(Long userId) {
        HorarioActualResponse horario = getHorarioActualByUserId(userId);
        return toPdf(horario);
    }

    public boolean hasHorarioActual(Long userId) {
        HorarioActualResponse horario = getHorarioActualByUserId(userId);
        return horario != null && horario.clases() != null && !horario.clases().isEmpty();
    }

    private List<HorarioClaseResponse> parseHorario(OfertaMateriaRepository.HorarioOfertaRow row) {
        List<HorarioClaseResponse> clases = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(row.getHorarioJson());
            if (!array.isArray()) {
                return clases;
            }

            for (JsonNode slot : array) {
                String dia = text(slot, "dia");
                String horaInicio = text(slot, "inicio");
                if (horaInicio == null) {
                    horaInicio = text(slot, "hora_inicio");
                }
                String horaFin = text(slot, "fin");
                if (horaFin == null) {
                    horaFin = text(slot, "hora_fin");
                }

                if (dia == null || horaInicio == null || horaFin == null) {
                    continue;
                }

                clases.add(new HorarioClaseResponse(
                    row.getMateriaNombre(),
                    row.getParalelo(),
                    dia,
                    horaInicio,
                    horaFin,
                    row.getDocente(),
                    row.getAula()
                ));
            }
        } catch (Exception ignored) {
            // Si una oferta tiene horario_json invalido, se ignora para no romper toda la vista.
        }
        return clases;
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private String toCsv(HorarioActualResponse horario) {
        StringBuilder builder = new StringBuilder();
        builder.append("Materia,Paralelo,Dia,HoraInicio,HoraFin,Aula,Docente\n");

        if (horario == null || horario.clases() == null || horario.clases().isEmpty()) {
            return builder.toString();
        }

        for (HorarioClaseResponse clase : horario.clases()) {
            builder
                .append(csv(clase.materia()))
                .append(',')
                .append(csv(clase.paralelo()))
                .append(',')
                .append(csv(clase.dia()))
                .append(',')
                .append(csv(clase.horaInicio()))
                .append(',')
                .append(csv(clase.horaFin()))
                .append(',')
                .append(csv(clase.aula()))
                .append(',')
                .append(csv(clase.docente()))
                .append('\n');
        }

        return builder.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.trim();
        if (sanitized.isEmpty()) {
            return "";
        }
        String escaped = sanitized.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private byte[] toPdf(HorarioActualResponse horario) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9f);

            document.add(new Paragraph("Horario academico", titleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] { 3.2f, 1.2f, 1.6f, 1.4f, 1.4f, 1.6f });

            addHeaderCell(table, "Materia", headerFont);
            addHeaderCell(table, "Paralelo", headerFont);
            addHeaderCell(table, "Dia", headerFont);
            addHeaderCell(table, "Inicio", headerFont);
            addHeaderCell(table, "Fin", headerFont);
            addHeaderCell(table, "Aula", headerFont);

            if (horario != null && horario.clases() != null) {
                for (HorarioClaseResponse clase : horario.clases()) {
                    addBodyCell(table, safeText(clase.materia()), cellFont);
                    addBodyCell(table, safeText(clase.paralelo()), cellFont);
                    addBodyCell(table, safeText(clase.dia()), cellFont);
                    addBodyCell(table, safeText(clase.horaInicio()), cellFont);
                    addBodyCell(table, safeText(clase.horaFin()), cellFont);
                    addBodyCell(table, safeText(clase.aula()), cellFont);
                }
            }

            document.add(table);
        } catch (Exception ex) {
            return new byte[0];
        } finally {
            document.close();
        }
        return outputStream.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }
}
