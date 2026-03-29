package com.easyschedule.backend.estudiante.repository;

import com.easyschedule.backend.estudiante.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByUsernameIgnoreCase(String username);
    Optional<Estudiante> findByCorreoIgnoreCase(String correo);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCarnetIdentidadIgnoreCase(String carnetIdentidad);
}
