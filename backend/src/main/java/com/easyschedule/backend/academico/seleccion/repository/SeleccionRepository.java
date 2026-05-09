package com.easyschedule.backend.academico.seleccion.repository;

import com.easyschedule.backend.academico.seleccion.model.Seleccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeleccionRepository extends JpaRepository<Seleccion, Long> {

    List<Seleccion> findByEstudianteId(Long estudianteId);

    Optional<Seleccion> findByEstudianteIdAndOfertaMateriaId(Long estudianteId, Long ofertaMateriaId);

    @Query("SELECT s FROM Seleccion s WHERE s.estudiante.id = :estudianteId AND s.ofertaMateria.mallaMateriaId = :mallaMateriaId")
    Optional<Seleccion> findByEstudianteIdAndMallaMateriaId(@Param("estudianteId") Long estudianteId, @Param("mallaMateriaId") Long mallaMateriaId);

    void deleteByEstudianteId(Long estudianteId);
}
