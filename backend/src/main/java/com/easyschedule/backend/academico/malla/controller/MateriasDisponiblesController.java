package com.easyschedule.backend.academico.malla.controller;

import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import com.easyschedule.backend.academico.malla.dto.MateriaDisponibleConOfertasResponse;
import com.easyschedule.backend.academico.malla.service.MallaDisponibilidadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/materias")
public class MateriasDisponiblesController {

    private final MallaDisponibilidadService disponibilidadService;

    public MateriasDisponiblesController(MallaDisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/disponibles")
    public List<MallaMateriaResponse> getMateriasDisponibles(
            @RequestParam("mallaId") Long mallaId,
            @RequestParam("userId") Long userId) {
        return disponibilidadService.getMateriasDisponibles(mallaId, userId);
    }

    @GetMapping("/disponibles/ofertas")
    public List<MateriaDisponibleConOfertasResponse> getMateriasDisponiblesConOfertas(
            @RequestParam("mallaId") Long mallaId,
            @RequestParam("userId") Long userId) {
        return disponibilidadService.getMateriasDisponiblesConOfertas(mallaId, userId);
    }
}
