package com.easyschedule.backend.academico.horario.controller;

import com.easyschedule.backend.academico.horario.dto.HorarioGeneradoResponse;
import com.easyschedule.backend.academico.horario.dto.HorarioGeneradorRequest;
import com.easyschedule.backend.academico.horario.service.HorarioGeneradorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
public class HorarioGeneradorController {

    private final HorarioGeneradorService horarioGeneradorService;

    public HorarioGeneradorController(HorarioGeneradorService horarioGeneradorService) {
        this.horarioGeneradorService = horarioGeneradorService;
    }

    @PostMapping("/generar")
    public ResponseEntity<List<HorarioGeneradoResponse>> generarHorarios(@RequestBody HorarioGeneradorRequest request) {
        List<HorarioGeneradoResponse> mejoresHorarios = horarioGeneradorService.generarHorarios(request);
        return ResponseEntity.ok(mejoresHorarios);
    }
}
