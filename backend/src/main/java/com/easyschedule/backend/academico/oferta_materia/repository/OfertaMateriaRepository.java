package com.easyschedule.backend.academico.oferta_materia.repository;

import com.easyschedule.backend.academico.oferta_materia.model.OfertaMateria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfertaMateriaRepository extends JpaRepository<OfertaMateria, Long> {

    List<OfertaMateria> findByMallaMateriaId(Long mallaMateriaId);

    @Query(value = "SELECT o.* FROM ofertas o JOIN malla_materia mm ON o.malla_materia_id = mm.id WHERE mm.materia_id = :materiaId", nativeQuery = true)
    List<OfertaMateria> findByMateriaId(@Param("materiaId") Long materiaId);

    Optional<OfertaMateria> findByMallaMateriaIdAndSemestreAndParalelo(
        Long mallaMateriaId,
        String semestre,
        String paralelo
    );

    interface OfertaListRow {
        Long getId();
        String getCodigoMateria();
        String getNombreMateria();
        String getSemestre();
        String getParalelo();
        String getDocente();
        String getAula();
    }

    @Query(
        value = """
            SELECT
                o.id AS id,
                ma.codigo AS codigoMateria,
                ma.nombre AS nombreMateria,
                o.semestre AS semestre,
                o.paralelo AS paralelo,
                o.docente AS docente,
                o.aula AS aula
            FROM ofertas o
            JOIN malla_materia mm ON mm.id = o.malla_materia_id
            JOIN materias ma ON ma.id = mm.materia_id
            WHERE mm.malla_id = :mallaId
                AND (:search IS NULL
                    OR LOWER(ma.codigo) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(ma.nombre) LIKE LOWER(CONCAT('%', :search, '%')))
                AND (:semestre IS NULL OR o.semestre = :semestre)
                AND (:paralelo IS NULL OR o.paralelo = :paralelo)
            ORDER BY ma.nombre ASC, o.paralelo ASC, o.id ASC
            """,
        nativeQuery = true
    )
    List<OfertaListRow> findOfertasByFilters(
        @Param("mallaId") Long mallaId,
        @Param("search") String search,
        @Param("semestre") String semestre,
        @Param("paralelo") String paralelo
    );

    @Query(
        value = """
            SELECT DISTINCT o.semestre FROM ofertas o
            JOIN malla_materia mm ON mm.id = o.malla_materia_id
            WHERE mm.malla_id = :mallaId
            ORDER BY o.semestre DESC
            """,
        nativeQuery = true
    )
    List<String> findDistinctSemestresByMallaId(@Param("mallaId") Long mallaId);

    @Query(
        value = """
            SELECT DISTINCT o.paralelo FROM ofertas o
            JOIN malla_materia mm ON mm.id = o.malla_materia_id
            WHERE mm.malla_id = :mallaId
            ORDER BY o.paralelo ASC
            """,
        nativeQuery = true
    )
    List<String> findDistinctParalelosByMallaId(@Param("mallaId") Long mallaId);

    interface HorarioOfertaRow {
        Long getOfertaId();
        String getSemestre();
        String getParalelo();
        String getMateriaNombre();
        String getHorarioJson();
        String getDocente();
        String getAula();
        Integer getCreditos();
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
                o.aula AS aula,
                ma.creditos AS creditos
            FROM ofertas o
            JOIN malla_materia mm ON mm.id = o.malla_materia_id
            JOIN materias ma ON ma.id = mm.materia_id
            JOIN estado_materia_estudiante eme
                ON eme.malla_materia_id = mm.id
                AND eme.user_id = :userId
                        LEFT JOIN toma_materia_estudiante tme
                                ON tme.oferta_id = o.id
                                AND tme.user_id = :userId
            WHERE mm.malla_id = :mallaId
                            AND LOWER(COALESCE(tme.estado, '')) IN ('inscrita', 'cursando')
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