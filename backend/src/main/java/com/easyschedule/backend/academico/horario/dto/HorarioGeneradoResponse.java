package com.easyschedule.backend.academico.horario.dto;

import java.util.List;

public record HorarioGeneradoResponse(
    double puntajeTotal,
    List<HorarioClaseResponse> clases
) implements Comparable<HorarioGeneradoResponse> {
    
    @Override
    public int compareTo(HorarioGeneradoResponse o) {
        return Double.compare(this.puntajeTotal, o.puntajeTotal);
    }
}
