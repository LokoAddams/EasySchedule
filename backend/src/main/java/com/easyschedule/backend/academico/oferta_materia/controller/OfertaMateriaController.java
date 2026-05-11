package com.easyschedule.backend.academico.oferta_materia.controller;

import com.easyschedule.backend.academico.oferta_materia.dto.OfertaDetalleResponse;
import com.easyschedule.backend.academico.oferta_materia.service.OfertaMateriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.easyschedule.backend.academico.oferta_materia.dto.Importacion.OfertaImportResultResponse;
import com.easyschedule.backend.academico.oferta_materia.service.OfertaMateriaImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/academico/ofertas")
public class OfertaMateriaController {

    private final OfertaMateriaService ofertaMateriaService;
    private final OfertaMateriaImportService ofertaMateriaImportService;

    public OfertaMateriaController(
        OfertaMateriaService ofertaMateriaService,
        OfertaMateriaImportService ofertaMateriaImportService
    ) {
        this.ofertaMateriaService = ofertaMateriaService;
        this.ofertaMateriaImportService = ofertaMateriaImportService;
    }

    @GetMapping("/detalles/{mallaMateriaId}")
    public OfertaDetalleResponse getDetallesMateria(@PathVariable("mallaMateriaId") Long mallaMateriaId) {
        return ofertaMateriaService.getDetalleParaInscripcion(mallaMateriaId);
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OfertaImportResultResponse importarOfertas(
        @RequestParam("mallaId") Long mallaId,
        @RequestParam("file") MultipartFile file
    ) {
        return ofertaMateriaImportService.importCsv(mallaId, file);
    }
}
