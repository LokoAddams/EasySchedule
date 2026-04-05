package com.easyschedule.backend.academico.malla.repository;

import com.easyschedule.backend.academico.malla.model.Malla;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MallaRepository extends JpaRepository<Malla, Long> {
    List<Malla> findByCarreraIdAndActiveTrueOrderByVersionDesc(Long carreraId);
    List<Malla> findByActiveTrueOrderByVersionDesc();
    java.util.Optional<Malla> findByIdAndActiveTrue(Long id);
}
