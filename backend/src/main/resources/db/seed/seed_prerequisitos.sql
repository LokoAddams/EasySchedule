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
family_subjects AS (
	SELECT
		familias.prefix,
		familias.prefix || LPAD(((stem_ord - 1) * 4 + nivel.nivel_ord)::text, 2, '0') AS materia_codigo,
		((stem_ord - 1) * 4 + nivel.nivel_ord) AS ordinality,
		stem_ord,
		nivel.nivel_ord
	FROM familias
	CROSS JOIN LATERAL unnest(familias.stems) WITH ORDINALITY AS stem_data(stem, stem_ord)
	CROSS JOIN (VALUES (1), (2), (3), (4)) AS nivel(nivel_ord)
),
malla_subjects AS (
	SELECT
		mm.malla_id,
		mm.id AS malla_materia_id,
		fs.ordinality,
		fs.stem_ord,
		fs.nivel_ord,
		fs.prefix,
		c.codigo AS carrera_codigo
	FROM malla_materia mm
	JOIN materias m ON m.id = mm.materia_id
	JOIN mallas ma ON ma.id = mm.malla_id
	JOIN carreras c ON c.id = ma.carrera_id
	JOIN career_family cf ON cf.career_codigo = c.codigo
 	JOIN family_subjects fs ON fs.prefix = cf.family AND fs.materia_codigo = m.codigo
),
capstone_subjects AS (
	SELECT
		mm.malla_id,
		mm.id AS malla_materia_id,
		m.codigo AS materia_codigo,
		c.codigo AS carrera_codigo
	FROM malla_materia mm
	JOIN materias m ON m.id = mm.materia_id
	JOIN mallas ma ON ma.id = mm.malla_id
	JOIN carreras c ON c.id = ma.carrera_id
	WHERE m.codigo IN ('TGRI', 'TGRII')
),
vertical_rules AS (
	SELECT
		cur.malla_materia_id AS current_malla_materia_id,
		pre.malla_materia_id AS prereq_malla_materia_id
	FROM malla_subjects cur
	JOIN malla_subjects pre
		ON pre.malla_id = cur.malla_id
	   AND pre.carrera_codigo = cur.carrera_codigo
	   AND pre.stem_ord = cur.stem_ord
	   AND pre.nivel_ord = cur.nivel_ord - 1
	WHERE cur.nivel_ord > 1
),
horizontal_rules AS (
	SELECT
		cur.malla_materia_id AS current_malla_materia_id,
		pre.malla_materia_id AS prereq_malla_materia_id
	FROM malla_subjects cur
	JOIN malla_subjects pre
		ON pre.malla_id = cur.malla_id
	   AND pre.carrera_codigo = cur.carrera_codigo
	   AND (
			(cur.ordinality = 16 AND pre.ordinality = 11)
			OR (cur.ordinality = 20 AND pre.ordinality = 15)
			OR (cur.ordinality = 28 AND pre.ordinality = 24)
			OR (cur.ordinality = 32 AND pre.ordinality = 28)
			OR (cur.ordinality = 36 AND pre.ordinality = 32)
			OR (cur.ordinality = 40 AND pre.ordinality = 36)
		)
),
capstone_bridge_rules AS (
	SELECT
		tgi.malla_materia_id AS current_malla_materia_id,
		base.max_malla_materia_id AS prereq_malla_materia_id
	FROM capstone_subjects tgi
	JOIN (
		SELECT
			malla_subjects.malla_id,
			MAX(malla_subjects.ordinality) AS max_ordinality
		FROM malla_subjects
		GROUP BY malla_subjects.malla_id
	) last_ord ON last_ord.malla_id = tgi.malla_id
	JOIN (
		SELECT
			ms.malla_id,
			MAX(ms.malla_materia_id) AS max_malla_materia_id
		FROM malla_subjects ms
		JOIN (
			SELECT malla_id, MAX(ordinality) AS max_ordinality
			FROM malla_subjects
			GROUP BY malla_id
		) mo ON mo.malla_id = ms.malla_id AND mo.max_ordinality = ms.ordinality
		GROUP BY ms.malla_id
	) base ON base.malla_id = tgi.malla_id
	WHERE tgi.materia_codigo = 'TGRI'
),
capstone_terminal_rules AS (
	SELECT
		tg2.malla_materia_id AS current_malla_materia_id,
		tg1.malla_materia_id AS prereq_malla_materia_id
	FROM capstone_subjects tg2
	JOIN capstone_subjects tg1
		ON tg1.malla_id = tg2.malla_id
	   AND tg1.materia_codigo = 'TGRI'
	WHERE tg2.materia_codigo = 'TGRII'
)
INSERT INTO prerequisitos (malla_materia_id, prereq_malla_materia_id)
SELECT DISTINCT
	r.current_malla_materia_id,
	r.prereq_malla_materia_id
FROM (
	SELECT * FROM vertical_rules
	UNION ALL
	SELECT * FROM horizontal_rules
	UNION ALL
	SELECT * FROM capstone_bridge_rules
	UNION ALL
	SELECT * FROM capstone_terminal_rules
) r
WHERE r.current_malla_materia_id <> r.prereq_malla_materia_id
ON CONFLICT (malla_materia_id, prereq_malla_materia_id) DO NOTHING;

COMMIT;
