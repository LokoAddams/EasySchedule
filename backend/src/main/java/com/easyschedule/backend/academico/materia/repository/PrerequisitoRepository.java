package com.easyschedule.backend.academico.materia.repository;

import com.easyschedule.backend.academico.materia.model.Prerequisito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrerequisitoRepository extends JpaRepository<Prerequisito, Long> {
}
