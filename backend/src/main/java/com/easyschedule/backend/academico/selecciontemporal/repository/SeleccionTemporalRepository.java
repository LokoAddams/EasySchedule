package com.easyschedule.backend.academico.selecciontemporal.repository;

import com.easyschedule.backend.academico.selecciontemporal.model.SeleccionTemporal;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeleccionTemporalRepository extends JpaRepository<SeleccionTemporal, Long> {

    List<SeleccionTemporal> findByEstudianteId(Long estudianteId);

    Optional<SeleccionTemporal> findByEstudianteIdAndOfertaMateriaId(Long estudianteId, Long ofertaMateriaId);

    @Query("SELECT s FROM SeleccionTemporal s WHERE s.estudiante.id = :estudianteId AND s.ofertaMateria.mallaMateriaId = :mallaMateriaId")
    Optional<SeleccionTemporal> findByEstudianteIdAndMallaMateriaId(@Param("estudianteId") Long estudianteId, @Param("mallaMateriaId") Long mallaMateriaId);

    void deleteByEstudianteId(Long estudianteId);
}
