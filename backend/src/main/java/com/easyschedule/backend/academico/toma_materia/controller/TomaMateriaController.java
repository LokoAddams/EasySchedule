package com.easyschedule.backend.academico.toma_materia.controller;

import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaRequest;
import com.easyschedule.backend.academico.toma_materia.dto.TomaMateriaResponse;
import com.easyschedule.backend.academico.toma_materia.service.TomaMateriaService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/academico/toma-materias")
public class TomaMateriaController {

    private final TomaMateriaService tomaMateriaService;

    public TomaMateriaController(TomaMateriaService tomaMateriaService) {
        this.tomaMateriaService = tomaMateriaService;
    }

    @GetMapping
    public List<TomaMateriaResponse> listByCurrentUser(Principal principal) {
        return tomaMateriaService.listByUserId(getAuthenticatedUserId(principal));
    }

    @PostMapping
    public List<TomaMateriaResponse> saveByCurrentUser(@Valid @RequestBody TomaMateriaRequest request, Principal principal) {
        return tomaMateriaService.saveByUserId(getAuthenticatedUserId(principal), request);
    }

    @DeleteMapping("/oferta/{ofertaId}")
    public void deleteByOferta(@PathVariable("ofertaId") Long ofertaId, Principal principal) {
        tomaMateriaService.deleteByUserIdAndOfertaId(getAuthenticatedUserId(principal), ofertaId);
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
