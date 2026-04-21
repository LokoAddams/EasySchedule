BEGIN;

WITH familias(prefix, stems) AS (
	VALUES
		('SIS', ARRAY['Programacion', 'Bases de Datos', 'Redes de Computadoras', 'Ingenieria de Software', 'Sistemas Operativos', 'Arquitectura de Computadoras', 'Analisis y Diseno de Sistemas', 'Inteligencia Artificial', 'Desarrollo Web', 'Seguridad Informatica']::text[]),
		('BUS', ARRAY['Contabilidad', 'Finanzas', 'Marketing', 'Recursos Humanos', 'Economia', 'Costos', 'Auditoria', 'Comercio Internacional', 'Gestion Comercial', 'Emprendimiento']::text[]),
		('CON', ARRAY['Dibujo Tecnico', 'Topografia', 'Resistencia de Materiales', 'Materiales de Construccion', 'Hidraulica', 'Estructuras', 'Geotecnia', 'Gestion de Obras', 'Proyectos Arquitectonicos', 'Instalaciones']::text[]),
		('SOC', ARRAY['Derecho', 'Psicologia', 'Sociologia', 'Filosofia', 'Investigacion Social', 'Intervencion Comunitaria', 'Orientacion Vocacional', 'Legislacion', 'Etica Profesional', 'Practica Supervisada']::text[]),
		('SAL', ARRAY['Anatomia', 'Fisiologia', 'Bioquimica', 'Microbiologia', 'Patologia', 'Farmacologia', 'Semiologia', 'Medicina Interna', 'Pediatria', 'Salud Publica']::text[])
),
career_family(career_codigo, family) AS (
	VALUES
		('SISCOMP', 'SIS'),
		('SIS', 'SIS'),
		('INGCOM', 'BUS'),
		('INGFIN', 'BUS'),
		('ADMEMP', 'BUS'),
		('CIV', 'CON'),
		('ARQ', 'CON'),
		('IND', 'CON'),
		('PSI', 'SOC'),
		('DER', 'SOC'),
		('MED', 'SAL')
),
career_load(career_codigo, materias_legacy, materias_modern, semestres_legacy, semestres_modern) AS (
	VALUES
		('SISCOMP', 36, 40, 8, 9),
		('SIS', 34, 38, 8, 9),
		('INGCOM', 30, 34, 8, 9),
		('INGFIN', 32, 36, 8, 9),
		('ADMEMP', 30, 34, 8, 8),
		('CIV', 36, 40, 9, 10),
		('ARQ', 34, 38, 9, 10),
		('IND', 32, 36, 8, 9),
		('PSI', 30, 34, 8, 8),
		('DER', 32, 36, 8, 9),
		('MED', 40, 40, 10, 10)
),
career_capstone(career_codigo, capstone_count) AS (
	VALUES
		('SISCOMP', 2),
		('SIS', 2),
		('INGCOM', 2),
		('INGFIN', 2),
		('CIV', 2),
		('ARQ', 2),
		('IND', 2)
),
family_subjects AS (
	SELECT
		familias.prefix,
		familias.prefix || LPAD(((stem_ord - 1) * 4 + nivel.nivel_ord)::text, 2, '0') AS materia_codigo,
		((stem_ord - 1) * 4 + nivel.nivel_ord) AS ordinality
	FROM familias
	CROSS JOIN LATERAL unnest(familias.stems) WITH ORDINALITY AS stem_data(stem, stem_ord)
	CROSS JOIN (VALUES (1), (2), (3), (4)) AS nivel(nivel_ord)
),
managed_mallas AS (
	SELECT m.id AS malla_id
	FROM mallas m
	JOIN carreras c ON c.id = m.carrera_id
	WHERE c.codigo IN ('SISCOMP', 'SIS', 'INGCOM', 'INGFIN', 'ADMEMP', 'CIV', 'ARQ', 'IND', 'PSI', 'DER', 'MED')
),
deleted_prerequisitos AS (
	DELETE FROM prerequisitos p
	WHERE EXISTS (
		SELECT 1
		FROM malla_materia mm
		JOIN managed_mallas mmg ON mmg.malla_id = mm.malla_id
		WHERE mm.id = p.malla_materia_id OR mm.id = p.prereq_malla_materia_id
	)
	RETURNING 1
),
malla_targets AS (
	SELECT
		m.id AS malla_id,
		c.codigo AS career_codigo,
		CASE
			WHEN m.version ~ '^[0-9]+$' AND m.version::int >= 2022 THEN cl.materias_modern
			ELSE cl.materias_legacy
		END AS materias_objetivo,
		CASE
			WHEN m.version ~ '^[0-9]+$' AND m.version::int >= 2022 THEN cl.semestres_modern
			ELSE cl.semestres_legacy
		END AS semestres_objetivo,
		COALESCE(cc.capstone_count, 0) AS capstone_count
	FROM mallas m
	JOIN carreras c ON c.id = m.carrera_id
	JOIN career_load cl ON cl.career_codigo = c.codigo
	LEFT JOIN career_capstone cc ON cc.career_codigo = c.codigo
),
plan_mallas AS (
	SELECT
		mt.malla_id,
		mt.career_codigo,
		mt.materias_objetivo,
		mt.semestres_objetivo,
		mt.capstone_count,
		GREATEST(mt.semestres_objetivo - 1, 1) AS semestres_base_objetivo,
		GREATEST(mt.materias_objetivo - mt.capstone_count, 1) AS materias_familia_objetivo,
		fs.materia_codigo,
		fs.ordinality
	FROM malla_targets mt
	JOIN career_family cf ON cf.career_codigo = mt.career_codigo
	JOIN family_subjects fs ON fs.prefix = cf.family
),
ranked_plan AS (
	SELECT
		plan_mallas.*,
		ROW_NUMBER() OVER (
			PARTITION BY plan_mallas.malla_id
			ORDER BY
				CASE
					WHEN plan_mallas.semestres_objetivo >= 9 AND plan_mallas.ordinality BETWEEN 25 AND 36 THEN plan_mallas.ordinality - 1
					ELSE plan_mallas.ordinality
				END,
				plan_mallas.ordinality
		) AS materia_posicion
	FROM plan_mallas
),
selected_family_plan AS (
	SELECT
		ranked_plan.malla_id,
		ranked_plan.career_codigo,
		ranked_plan.materia_codigo,
		LEAST(
			ranked_plan.semestres_base_objetivo,
			CEIL((ranked_plan.materia_posicion::numeric * ranked_plan.semestres_base_objetivo::numeric) / ranked_plan.materias_familia_objetivo::numeric)::smallint
		) AS semestre_sugerido
	FROM ranked_plan
	WHERE ranked_plan.materia_posicion <= ranked_plan.materias_familia_objetivo
),
selected_capstone_plan AS (
	SELECT
		mt.malla_id,
		mt.career_codigo,
		'TGRI'::text AS materia_codigo,
		(mt.semestres_objetivo - 1)::smallint AS semestre_sugerido
	FROM malla_targets mt
	WHERE mt.capstone_count = 2

	UNION ALL

	SELECT
		mt.malla_id,
		mt.career_codigo,
		'TGRII'::text AS materia_codigo,
		mt.semestres_objetivo::smallint AS semestre_sugerido
	FROM malla_targets mt
	WHERE mt.capstone_count = 2
),
selected_plan AS (
	SELECT malla_id, materia_codigo, semestre_sugerido FROM selected_family_plan
	UNION ALL
	SELECT malla_id, materia_codigo, semestre_sugerido FROM selected_capstone_plan
),
upsert_malla_materia AS (
INSERT INTO malla_materia (malla_id, materia_id, semestre_sugerido)
SELECT
	selected_plan.malla_id,
	materias.id,
	selected_plan.semestre_sugerido
FROM selected_plan
JOIN materias ON materias.codigo = selected_plan.materia_codigo
ON CONFLICT (malla_id, materia_id) DO UPDATE
SET semestre_sugerido = EXCLUDED.semestre_sugerido
RETURNING 1
)
DELETE FROM malla_materia mm
WHERE EXISTS (SELECT 1 FROM managed_mallas m WHERE m.malla_id = mm.malla_id)
  AND NOT EXISTS (
	SELECT 1
	FROM selected_plan sp
	JOIN materias mat ON mat.codigo = sp.materia_codigo
	WHERE sp.malla_id = mm.malla_id
	  AND mat.id = mm.materia_id
  );

COMMIT;
