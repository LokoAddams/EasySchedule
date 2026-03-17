package com.easyschedule.backend.estudiante.controller;

import com.easyschedule.backend.auth.dto.RegistroRequest;
import com.easyschedule.backend.estudiante.dto.EstudianteResponse;
import com.easyschedule.backend.estudiante.dto.EstudianteUpdateRequest;
import com.easyschedule.backend.estudiante.service.EstudianteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping
    public List<EstudianteResponse> findAll() {
        return estudianteService.findAll();
    }

    @GetMapping("/{id}")
    public EstudianteResponse findById(@PathVariable Long id) {
        return estudianteService.findById(id);
    }

    @GetMapping("/perfil/{username}")
    public EstudianteResponse findProfileByUsername(@PathVariable String username) {
        return estudianteService.findByUsername(username);
    }

    @PutMapping("/{id}")
    public EstudianteResponse update(@PathVariable Long id, @RequestBody EstudianteUpdateRequest request) {
        return estudianteService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        estudianteService.delete(id);
    }

    @PostMapping("/registro")
    public ResponseEntity<EstudianteResponse> register(@Valid @RequestBody RegistroRequest request) {
        EstudianteResponse response = estudianteService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
