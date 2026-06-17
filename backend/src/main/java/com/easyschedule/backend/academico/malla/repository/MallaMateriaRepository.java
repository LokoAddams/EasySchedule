package com.easyschedule.backend.academico.malla.repository;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MallaMateriaRepository extends JpaRepository<MallaMateria, Long> {
	List<MallaMateria> findByMallaIdOrderBySemestreSugeridoAsc(Long mallaId);

	List<MallaMateria> findByMallaIdAndMateriaActiveTrueOrderBySemestreSugeridoAsc(Long mallaId);

	Optional<MallaMateria> findByMallaIdAndMateriaId(Long mallaId, Long materiaId);

	Optional<MallaMateria> findByMallaIdAndMateria_Codigo(Long mallaId, String codigo);
}
