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
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
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
}
