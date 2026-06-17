package com.easyschedule.backend.academico.oferta_materia.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record HorarioDto(
    String dia,
    
    @JsonAlias({"inicio", "horaInicio"})
    @JsonProperty("horaInicio")
    String horaInicio,
    
    @JsonAlias({"fin", "horaFin"})
    @JsonProperty("horaFin")
    String horaFin
) {}
