package com.easyschedule.backend.academico.materia.repository;

import com.easyschedule.backend.academico.materia.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
}
