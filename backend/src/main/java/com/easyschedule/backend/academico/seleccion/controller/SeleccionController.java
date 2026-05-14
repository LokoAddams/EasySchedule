package com.easyschedule.backend.academico.seleccion.controller;

import com.easyschedule.backend.academico.seleccion.dto.SeleccionRequest;
import com.easyschedule.backend.academico.seleccion.dto.SeleccionResponse;
import com.easyschedule.backend.academico.seleccion.service.SeleccionService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/academico/seleccion")
public class SeleccionController {

    private final SeleccionService seleccionService;

    public SeleccionController(SeleccionService seleccionService) {
        this.seleccionService = seleccionService;
    }

    @GetMapping
    public SeleccionResponse getSeleccion(Principal principal) {
        return seleccionService.getSeleccionByUserId(getAuthenticatedUserId(principal));
    }

    @PutMapping
    public SeleccionResponse saveSeleccion(@Valid @RequestBody SeleccionRequest request, Principal principal) {
        return seleccionService.saveSeleccionByUserId(getAuthenticatedUserId(principal), request);
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
