package com.easyschedule.backend.academico.materia.repository;

import com.easyschedule.backend.academico.materia.model.Prerequisito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrerequisitoRepository extends JpaRepository<Prerequisito, Long> {
    List<Prerequisito> findByMallaMateria_Id(Long mallaMateriaId);
}
