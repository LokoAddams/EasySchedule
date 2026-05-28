package com.easyschedule.backend.academico.materia.repository;

import com.easyschedule.backend.academico.materia.model.Prerequisito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PrerequisitoRepository extends JpaRepository<Prerequisito, Long> {
    List<Prerequisito> findByMallaMateria_Id(Long mallaMateriaId);

    Optional<Prerequisito> findByMallaMateria_IdAndPrerequisito_Id(Long mallaMateriaId, Long prerequisitoId);

    boolean existsByMallaMateria_IdAndPrerequisito_Id(Long mallaMateriaId, Long prerequisitoId);

    @Query("SELECT p FROM Prerequisito p WHERE p.mallaMateria.id = :mmId1 AND p.prerequisito.id = :mmId2 " +
           "OR p.mallaMateria.id = :mmId2 AND p.prerequisito.id = :mmId1")
    List<Prerequisito> findInversePair(@Param("mmId1") Long mmId1, @Param("mmId2") Long mmId2);

    void deleteByMallaMateria_IdAndPrerequisito_Id(Long mallaMateriaId, Long prerequisitoId);
}
