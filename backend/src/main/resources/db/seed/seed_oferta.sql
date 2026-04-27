BEGIN;

-- Genera oferta global por malla_materia activa sin choques por semestre en la misma malla.
-- Solo paralelo A para simplificar toma de materias base.
CREATE TEMP TABLE tmp_seed_ofertas ON COMMIT DROP AS
WITH oferta_base AS (
    SELECT
        mm.id AS malla_materia_id,
        mm.malla_id,
        mm.semestre_sugerido,
        ROW_NUMBER() OVER (
            PARTITION BY mm.malla_id, mm.semestre_sugerido
            ORDER BY mm.id
        ) AS orden_semestre
    FROM malla_materia mm
    JOIN mallas m ON m.id = mm.malla_id AND m.active = TRUE
    JOIN materias ma ON ma.id = mm.materia_id AND ma.active = TRUE
),
slots AS (
    SELECT 1 AS slot_id, 'Lunes'::TEXT AS dia1, 'Miercoles'::TEXT AS dia2, '07:00'::TEXT AS inicio, '08:30'::TEXT AS fin
    UNION ALL SELECT 2, 'Lunes', 'Miercoles', '08:45', '10:15'
    UNION ALL SELECT 3, 'Martes', 'Jueves', '07:00', '08:30'
    UNION ALL SELECT 4, 'Martes', 'Jueves', '08:45', '10:15'
    UNION ALL SELECT 5, 'Viernes', 'Viernes', '18:30', '20:00'
)
SELECT
    ob.malla_materia_id,
    '2026-1'::VARCHAR(30) AS semestre,
    'A'::VARCHAR(20) AS paralelo,
    jsonb_build_array(
        jsonb_build_object('dia', s.dia1, 'inicio', s.inicio, 'fin', s.fin),
        jsonb_build_object('dia', s.dia2, 'inicio', s.inicio, 'fin', s.fin)
    ) AS horario_json,
    'Docente Sem ' || ob.semestre_sugerido::TEXT || ' Slot ' || s.slot_id::TEXT AS docente,
    'A-' || LPAD(ob.semestre_sugerido::TEXT, 2, '0') || '-' || s.slot_id::TEXT AS aula
FROM oferta_base ob
JOIN slots s ON s.slot_id = ((ob.orden_semestre - 1) % 5) + 1;

UPDATE ofertas o
SET horario_json = s.horario_json,
    docente = s.docente,
    aula = s.aula,
    fecha_actualizacion = NOW()
FROM tmp_seed_ofertas s
WHERE o.malla_materia_id = s.malla_materia_id
  AND o.semestre = s.semestre
  AND o.paralelo = s.paralelo;

INSERT INTO ofertas (
    malla_materia_id,
    semestre,
    paralelo,
    horario_json,
    docente,
    aula,
    fecha_creacion,
    fecha_actualizacion
)
SELECT
    s.malla_materia_id,
    s.semestre,
    s.paralelo,
    s.horario_json,
    s.docente,
    s.aula,
    NOW(),
    NOW()
FROM tmp_seed_ofertas s
WHERE NOT EXISTS (
    SELECT 1
    FROM ofertas o
    WHERE o.malla_materia_id = s.malla_materia_id
      AND o.semestre = s.semestre
      AND o.paralelo = s.paralelo
);

COMMIT;
