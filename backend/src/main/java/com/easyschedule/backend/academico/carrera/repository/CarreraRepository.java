package com.easyschedule.backend.academico.carrera.repository;

import com.easyschedule.backend.academico.carrera.model.Carrera;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
	List<Carrera> findByUniversidadIdAndActiveTrueOrderByNombreAsc(Long universidadId);
	Optional<Carrera> findByIdAndActiveTrue(Long id);
}
