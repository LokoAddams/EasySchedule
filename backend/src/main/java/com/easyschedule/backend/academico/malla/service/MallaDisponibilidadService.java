package com.easyschedule.backend.academico.malla.service;

import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MallaDisponibilidadService {

    private final MallaService mallaService;

    public MallaDisponibilidadService(MallaService mallaService) {
        this.mallaService = mallaService;
    }

    public List<MallaMateriaResponse> getMateriasDisponibles(Long mallaId, Long userId) {
        List<MallaMateriaResponse> todasLasMaterias = mallaService.findMateriasByMalla(mallaId, userId);

        // Mapas para el algoritmo de grafos
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, List<Long>> adj = new HashMap<>();
        Map<Long, MallaMateriaResponse> materiaMap = new HashMap<>();

        // Inicialización
        for (MallaMateriaResponse materia : todasLasMaterias) {
            materiaMap.put(materia.id(), materia);
            inDegree.put(materia.id(), materia.prerequisitosIds() != null ? materia.prerequisitosIds().size() : 0);
            adj.putIfAbsent(materia.id(), new ArrayList<>());
            
            // Construir lista de adyacencia (de prerequisito -> dependiente)
            if (materia.prerequisitosIds() != null) {
                for (Long prereqId : materia.prerequisitosIds()) {
                    adj.putIfAbsent(prereqId, new ArrayList<>());
                    adj.get(prereqId).add(materia.id());
                }
            }
        }

        // Propagación de materias aprobadas
        Map<Long, Integer> effectiveInDegree = new HashMap<>(inDegree);
        for (MallaMateriaResponse materia : todasLasMaterias) {
            String estado = materia.estado();
            // Basado en el MCP, el estado para materias completadas es "aprobada"
            if ("aprobada".equalsIgnoreCase(estado)) {
                List<Long> dependientes = adj.get(materia.id());
                if (dependientes != null) {
                    for (Long depId : dependientes) {
                        effectiveInDegree.put(depId, effectiveInDegree.getOrDefault(depId, 0) - 1);
                    }
                }
            }
        }

        // Filtrar las materias que están disponibles
        return todasLasMaterias.stream()
            .filter(m -> {
                String estado = m.estado();
                // Está disponible si NO está "aprobada" ni "cursando"
                // (puede ser "pendiente" o null cuando no hay registro en DB)
                boolean isCompletadaOCursando = "aprobada".equalsIgnoreCase(estado) || "cursando".equalsIgnoreCase(estado);
                return !isCompletadaOCursando && effectiveInDegree.getOrDefault(m.id(), 0) <= 0;
            })
            .collect(Collectors.toList());
    }
}
