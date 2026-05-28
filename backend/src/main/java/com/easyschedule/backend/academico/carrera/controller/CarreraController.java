package com.easyschedule.backend.academico.carrera.controller;

import com.easyschedule.backend.academico.carrera.dto.CarreraRequest;
import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.service.CarreraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academico/carreras")
public class CarreraController {

	private final CarreraService carreraService;

	public CarreraController(CarreraService carreraService) {
		this.carreraService = carreraService;
	}

	@GetMapping
	public List<CarreraResponse> findByUniversidad(@RequestParam("universidadId") Long universidadId) {
		return carreraService.findActiveByUniversidad(universidadId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CarreraResponse create(@Valid @RequestBody CarreraRequest request) {
		return carreraService.createCarrera(request);
	}
}
