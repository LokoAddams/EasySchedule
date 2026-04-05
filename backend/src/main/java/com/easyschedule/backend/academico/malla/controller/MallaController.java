package com.easyschedule.backend.academico.malla.controller;

import com.easyschedule.backend.academico.malla.dto.MallaResponse;
import com.easyschedule.backend.academico.malla.service.MallaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academico/mallas")
public class MallaController {

	private final MallaService mallaService;

	public MallaController(MallaService mallaService) {
		this.mallaService = mallaService;
	}

	@GetMapping
	public List<MallaResponse> findByCarrera(@RequestParam("carreraId") Long carreraId) {
		return mallaService.findActiveByCarrera(carreraId);
	}
}
