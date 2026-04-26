package com.easyschedule.backend.academico.oferta_materia.repository;

import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfertaMateriaRepository extends JpaRepository<OfertaMateria, Long> {

    List<OfertaMateria> findByMallaMateriaId(Long mallaMateriaId);

    interface HorarioOfertaRow {
        Long getOfertaId();
        String getSemestre();
        String getParalelo();
        String getMateriaNombre();
        String getHorarioJson();
        String getDocente();
        String getAula();
    }

    @Query(
        value = """
            SELECT
                o.id AS ofertaId,
                o.semestre AS semestre,
                o.paralelo AS paralelo,
                ma.nombre AS materiaNombre,
                CAST(o.horario_json AS TEXT) AS horarioJson,
                o.docente AS docente,
                o.aula AS aula
            FROM ofertas o
            JOIN malla_materia mm ON mm.id = o.malla_materia_id
            JOIN materias ma ON ma.id = mm.materia_id
            JOIN estado_materia_estudiante eme
                ON eme.malla_materia_id = mm.id
                AND eme.user_id = :userId
            WHERE mm.malla_id = :mallaId
              AND mm.semestre_sugerido = :semestreActual
              AND LOWER(eme.estado) = 'cursando'
            ORDER BY ma.nombre ASC, o.paralelo ASC, o.id ASC
            """,
        nativeQuery = true
    )
    List<HorarioOfertaRow> findHorarioActualRows(
        @Param("userId") Long userId,
        @Param("mallaId") Long mallaId,
        @Param("semestreActual") Short semestreActual
    );
}
