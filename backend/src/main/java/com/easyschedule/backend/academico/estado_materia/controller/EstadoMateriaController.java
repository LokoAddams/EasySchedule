package com.easyschedule.backend.academico.estado_materia.controller;

import com.easyschedule.backend.academico.estado_materia.dto.EstadoMateriaRequest;
import com.easyschedule.backend.academico.estado_materia.dto.EstadoMateriaResponse;
import com.easyschedule.backend.academico.estado_materia.service.EstadoMateriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/academico/estados-materia")
public class EstadoMateriaController {

    private static final Logger log = LoggerFactory.getLogger(EstadoMateriaController.class);

    private final EstadoMateriaService estadoMateriaService;

    public EstadoMateriaController(EstadoMateriaService estadoMateriaService) {
        this.estadoMateriaService = estadoMateriaService;
    }

    @GetMapping
    public List<EstadoMateriaResponse> getEstados(Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        log.debug("[ESTADO_MATERIA_GET] request recibido | userId={}", userId);
        return estadoMateriaService.getEstadosByUserId(userId);
    }

    @GetMapping("/malla/{mallaId}")
    public List<EstadoMateriaResponse> getEstadosByMalla(
        @PathVariable("mallaId") Long mallaId,
        Principal principal
    ) {
        Long userId = getAuthenticatedUserId(principal);
        log.debug("[ESTADO_MATERIA_GET_MALLA] request recibido | userId={} mallaId={}", userId, mallaId);
        return estadoMateriaService.getEstadosByMalla(userId, mallaId);
    }

    @PostMapping
    public EstadoMateriaResponse saveEstado(
        @Valid @RequestBody EstadoMateriaRequest request,
        Principal principal
    ) {
        Long userId = getAuthenticatedUserId(principal);
        log.debug("[ESTADO_MATERIA_SAVE] request recibido | userId={} mallaMateriaId={} estado={}", userId, request.mallaMateriaId(), request.estado());
        log.info("[ESTADO_MATERIA_SAVE] intento de actualizar estado | userId={} mallaMateriaId={}", userId, request.mallaMateriaId());
        return estadoMateriaService.saveEstado(userId, request);
    }

    @PostMapping("/batch")
    public List<EstadoMateriaResponse> saveEstados(
        @Valid @RequestBody List<EstadoMateriaRequest> requests,
        Principal principal
    ) {
        Long userId = getAuthenticatedUserId(principal);
        log.debug("[ESTADO_MATERIA_BATCH] request recibido | userId={} cantidad={}", userId, requests.size());
        log.info("[ESTADO_MATERIA_BATCH] intento de actualizar estados en lote | userId={} cantidad={}", userId, requests.size());
        return estadoMateriaService.saveEstados(userId, requests);
    }

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }

        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }
    }
}
