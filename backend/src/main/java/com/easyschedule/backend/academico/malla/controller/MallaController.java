package com.easyschedule.backend.academico.malla.controller;

import com.easyschedule.backend.academico.malla.dto.MallaImportRequest;
import com.easyschedule.backend.academico.malla.dto.MallaImportResponse;
import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.service.MallaFileParserService;
import com.easyschedule.backend.academico.malla.service.MallaImportService;
import com.easyschedule.backend.academico.malla.service.MallaService;
import com.easyschedule.backend.academico.malla.dto.MallaMateriaResponse;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/academico/mallas")
public class MallaController {

    private static final Logger logger = LoggerFactory.getLogger(MallaController.class);

    private final MallaService mallaService;
    private final MallaFileParserService fileParserService;
    private final MallaImportService mallaImportService;

    public MallaController(MallaService mallaService, MallaFileParserService fileParserService, MallaImportService mallaImportService) {
        this.mallaService = mallaService;
        this.fileParserService = fileParserService;
        this.mallaImportService = mallaImportService;
    }

    @GetMapping
    public List<MallaResponse> findByCarrera(@RequestParam("carreraId") Long carreraId) {
        return mallaService.findActiveByCarrera(carreraId);
    }

    @GetMapping("/{mallaId}/materias")
    public List<MallaMateriaResponse> findMateriasByMalla(@PathVariable("mallaId") Long mallaId, Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        return mallaService.findMateriasByMalla(mallaId, userId);
    }

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }
    }

    @PostMapping("/importar")
    public MallaImportResponse importarMalla(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "carreraId", required = false) Long carreraId,
            @RequestParam(value = "nombre", defaultValue = "Malla Importada") String nombre,
            @RequestParam(value = "version", defaultValue = "1.0") String version) {
        logger.info("Solicitud de importacion: archivo={}, carreraId={}, nombre={}, version={}",
            file.getOriginalFilename(), carreraId, nombre, version);
        try {
            MallaImportRequest request = fileParserService.parseFile(file);
            if (carreraId != null) {
                request = new MallaImportRequest(nombre, version, carreraId, request.materias());
            }
            logger.debug("Request parseado: {}", request);
            return mallaImportService.importarMalla(request);
        } catch (IllegalArgumentException e) {
            logger.warn("Error de validacion en importacion: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error interno al importar malla: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al importar: " + e.getMessage());
        }
    }
}
