package com.easyschedule.backend.academico.universidad.controller;

import com.easyschedule.backend.academico.universidad.dto.UniversidadRequest;
import com.easyschedule.backend.academico.universidad.dto.UniversidadResponse;
import com.easyschedule.backend.academico.universidad.service.UniversidadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academico/universidades")
public class UniversidadController {

    private final UniversidadService universidadService;

    public UniversidadController(UniversidadService universidadService) {
        this.universidadService = universidadService;
    }

    @GetMapping
    public List<UniversidadResponse> findAll() {
        return universidadService.findAllActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UniversidadResponse create(@Valid @RequestBody UniversidadRequest request) {
        return universidadService.createUniversidad(request);
    }
}
