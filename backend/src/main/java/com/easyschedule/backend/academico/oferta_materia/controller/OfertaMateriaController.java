package com.easyschedule.backend.academico.oferta_materia.controller;

import com.easyschedule.backend.academico.oferta_materia.dto.OfertaDetalleResponse;
import com.easyschedule.backend.academico.oferta_materia.service.OfertaMateriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academico/ofertas")
public class OfertaMateriaController {

    private final OfertaMateriaService ofertaMateriaService;

    public OfertaMateriaController(OfertaMateriaService ofertaMateriaService) {
        this.ofertaMateriaService = ofertaMateriaService;
    }

    @GetMapping("/detalles/{mallaMateriaId}")
    public OfertaDetalleResponse getDetallesMateria(@PathVariable Long mallaMateriaId) {
        return ofertaMateriaService.getDetalleParaInscripcion(mallaMateriaId);
    }
}
