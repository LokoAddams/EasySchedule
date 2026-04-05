package com.easyschedule.backend.academico.universidad.repository;

import com.easyschedule.backend.academico.universidad.model.Universidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversidadRepository extends JpaRepository<Universidad, Long> {
    List<Universidad> findByActiveTrueOrderByNombreAsc();
    Optional<Universidad> findByIdAndActiveTrue(Long id);
}
