package com.easyschedule.backend.academico.seleccion.controller;

import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.service.SeleccionService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/academico/seleccion-temporal")
public class SeleccionController {

    private final SeleccionService seleccionService;

    public SeleccionController(SeleccionService seleccionService) {
        this.seleccionService = seleccionService;
    }

    @GetMapping
    public List<SeleccionResponse> listSelections(Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        return seleccionService.listByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeleccionResponse addSelection(Principal principal, @RequestBody SeleccionRequest request) {
        Long userId = getAuthenticatedUserId(principal);
        return seleccionService.addSelection(userId, request);
    }

    @DeleteMapping("/{ofertaMateriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSelection(Principal principal, @PathVariable Long ofertaMateriaId) {
        Long userId = getAuthenticatedUserId(principal);
        seleccionService.removeSelection(userId, ofertaMateriaId);
    }

    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearSelections(Principal principal) {
        Long userId = getAuthenticatedUserId(principal);
        seleccionService.clearSelections(userId);
    }

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida");
        }

        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida");
        }
    }
}
