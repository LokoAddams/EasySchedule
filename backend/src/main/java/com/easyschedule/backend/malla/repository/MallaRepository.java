package com.easyschedule.backend.malla.repository;

import com.easyschedule.backend.malla.model.Malla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MallaRepository extends JpaRepository<Malla, Long> {
	Optional<Malla> findByCarreraIgnoreCaseAndUniversidadIgnoreCaseAndVersionIgnoreCase(
		String carrera,
		String universidad,
		String version
	);
}
