package com.easyschedule.backend.academico.malla.repository;

import com.easyschedule.backend.academico.malla.model.MallaMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MallaMateriaRepository extends JpaRepository<MallaMateria, Long> {
	List<MallaMateria> findByMallaIdAndMateriaActiveTrueOrderBySemestreSugeridoAsc(Long mallaId);
}
