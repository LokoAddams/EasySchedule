package com.easyschedule.backend.academico.carrera.controller;

import com.easyschedule.backend.academico.carrera.dto.CarreraResponse;
import com.easyschedule.backend.academico.carrera.service.CarreraService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
}
