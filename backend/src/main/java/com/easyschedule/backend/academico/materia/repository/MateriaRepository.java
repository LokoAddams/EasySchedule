package com.easyschedule.backend.academico.materia.repository;

import com.easyschedule.backend.academico.materia.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    Optional<Materia> findByCodigo(String codigo);
}
