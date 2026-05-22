package com.easyschedule.backend.estudiante.service;

import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.malla.service.MallaService;
import com.easyschedule.backend.estudiante.model.Estudiante;
import com.easyschedule.backend.estudiante.repository.EstudianteRepository;
import com.easyschedule.backend.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class EstudianteMallaExportService {

    private final EstudianteRepository estudianteRepository;
    private final MallaService mallaService;

    public EstudianteMallaExportService(
            EstudianteRepository estudianteRepository,
            MallaService mallaService) {
        this.estudianteRepository = estudianteRepository;
        this.mallaService = mallaService;
    }

    public byte[] exportarMalla(Long estudianteId, String formato) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado con id: " + estudianteId));

        if (estudiante.getMalla() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe malla disponible para exportar");
        }

        Long mallaId = estudiante.getMalla().getId();

        // 🔥 ahora ya incluye estado
        List<MallaMateriaResponse> materias = mallaService.findMateriasByMalla(mallaId, estudianteId);

        StringBuilder csv = new StringBuilder();
        csv.append("Semestre,Codigo,Materia,Estado\n");

        for (MallaMateriaResponse materia : materias) {
            csv.append(materia.semestreSugerido() != null ? materia.semestreSugerido() : "")
               .append(",")
               .append(escapeCsv(materia.codigoMateria()))
               .append(",")
               .append(escapeCsv(materia.nombreMateria()))
               .append(",")
               .append(materia.estado() != null ? materia.estado() : "PENDIENTE")
               .append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}