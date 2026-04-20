package com.easyschedule.backend.academico.estado_materia.repository;

import com.easyschedule.backend.academico.estado_materia.model.EstadoMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoMateriaRepository extends JpaRepository<EstadoMateria, Long> {
    List<EstadoMateria> findByUserId(Long userId);
    Optional<EstadoMateria> findByUserIdAndMallaMateriaId(Long userId, Long mallaMateriaId);

    @Query(value = "SELECT e.* FROM estado_materia_estudiante e INNER JOIN malla_materia mm ON e.malla_materia_id = mm.id WHERE e.user_id = :userId AND mm.malla_id = :mallaId", nativeQuery = true)
    List<EstadoMateria> findByUserIdAndMallaId(@Param("userId") Long userId, @Param("mallaId") Long mallaId);
}
