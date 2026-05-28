package com.easyschedule.backend.academico.horario.dto;

import java.util.List;

public record ParaleloEstructuradoDTO(
    Long idOferta,
    Long mallaMateriaId,
    String nombreMateria,
    String paralelo,
    String docente,
    String aula,
    List<ClaseBloqueDTO> bloques
) {}
