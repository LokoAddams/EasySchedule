BEGIN;

WITH familias(prefix, stems) AS (
	VALUES
		('SIS', ARRAY[
			'Programacion',
			'Bases de Datos',
			'Redes de Computadoras',
			'Ingenieria de Software',
			'Sistemas Operativos',
			'Arquitectura de Computadoras',
			'Analisis y Diseno de Sistemas',
			'Inteligencia Artificial',
			'Desarrollo Web',
			'Seguridad Informatica'
		]::text[]),
		('BUS', ARRAY[
			'Contabilidad',
			'Finanzas',
			'Marketing',
			'Recursos Humanos',
			'Economia',
			'Costos',
			'Auditoria',
			'Comercio Internacional',
			'Gestion Comercial',
			'Emprendimiento'
		]::text[]),
		('CON', ARRAY[
			'Dibujo Tecnico',
			'Topografia',
			'Resistencia de Materiales',
			'Materiales de Construccion',
			'Hidraulica',
			'Estructuras',
			'Geotecnia',
			'Gestion de Obras',
			'Proyectos Arquitectonicos',
			'Instalaciones'
		]::text[]),
		('SOC', ARRAY[
			'Derecho',
			'Psicologia',
			'Sociologia',
			'Filosofia',
			'Investigacion Social',
			'Intervencion Comunitaria',
			'Orientacion Vocacional',
			'Legislacion',
			'Etica Profesional',
			'Practica Supervisada'
		]::text[]),
		('SAL', ARRAY[
			'Anatomia',
			'Fisiologia',
			'Bioquimica',
			'Microbiologia',
			'Patologia',
			'Farmacologia',
			'Semiologia',
			'Medicina Interna',
			'Pediatria',
			'Salud Publica'
		]::text[])
)
INSERT INTO materias (codigo, nombre, creditos, active)
SELECT
	familias.prefix || LPAD(((stem_ord - 1) * 4 + nivel.nivel_ord)::text, 2, '0') AS codigo,
	CASE nivel.nivel_ord
		WHEN 1 THEN 'Fundamentos de ' || stem
		WHEN 2 THEN stem || ' Aplicada'
		WHEN 3 THEN stem || ' Avanzada'
		ELSE 'Taller Integrador de ' || stem
	END AS nombre,
	CASE
		WHEN familias.prefix = 'SIS' AND stem IN ('Programacion', 'Bases de Datos', 'Ingenieria de Software', 'Sistemas Operativos', 'Arquitectura de Computadoras', 'Analisis y Diseno de Sistemas') THEN 4
		WHEN familias.prefix = 'CON' AND stem IN ('Resistencia de Materiales', 'Estructuras', 'Geotecnia', 'Gestion de Obras') THEN 4
		WHEN familias.prefix = 'SAL' AND stem IN ('Anatomia', 'Fisiologia', 'Bioquimica', 'Patologia', 'Farmacologia', 'Medicina Interna') THEN 4
		ELSE 3
	END AS creditos,
	TRUE AS active
FROM familias
CROSS JOIN LATERAL unnest(familias.stems) WITH ORDINALITY AS stem_data(stem, stem_ord)
CROSS JOIN (VALUES (1, 'I'), (2, 'II'), (3, 'III'), (4, 'IV')) AS nivel(nivel_ord, nivel_nombre)
ORDER BY familias.prefix, stem_ord, nivel.nivel_ord
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    creditos = EXCLUDED.creditos,
    active = EXCLUDED.active;

INSERT INTO materias (codigo, nombre, creditos, active)
VALUES
	('TGRI', 'Taller de Grado I', 4, TRUE),
	('TGRII', 'Taller de Grado II', 4, TRUE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
	creditos = EXCLUDED.creditos,
	active = EXCLUDED.active;

WITH familias(prefix, stems) AS (
	VALUES
		('SIS', ARRAY['Programacion', 'Bases de Datos', 'Redes de Computadoras', 'Ingenieria de Software', 'Sistemas Operativos', 'Arquitectura de Computadoras', 'Analisis y Diseno de Sistemas', 'Inteligencia Artificial', 'Desarrollo Web', 'Seguridad Informatica']::text[]),
		('BUS', ARRAY['Contabilidad', 'Finanzas', 'Marketing', 'Recursos Humanos', 'Economia', 'Costos', 'Auditoria', 'Comercio Internacional', 'Gestion Comercial', 'Emprendimiento']::text[]),
		('CON', ARRAY['Dibujo Tecnico', 'Topografia', 'Resistencia de Materiales', 'Materiales de Construccion', 'Hidraulica', 'Estructuras', 'Geotecnia', 'Gestion de Obras', 'Proyectos Arquitectonicos', 'Instalaciones']::text[]),
		('SOC', ARRAY['Derecho', 'Psicologia', 'Sociologia', 'Filosofia', 'Investigacion Social', 'Intervencion Comunitaria', 'Orientacion Vocacional', 'Legislacion', 'Etica Profesional', 'Practica Supervisada']::text[]),
		('SAL', ARRAY['Anatomia', 'Fisiologia', 'Bioquimica', 'Microbiologia', 'Patologia', 'Farmacologia', 'Semiologia', 'Medicina Interna', 'Pediatria', 'Salud Publica']::text[])
)
UPDATE materias
SET active = FALSE
WHERE LEFT(codigo, 3) IN ('SIS', 'BUS', 'CON', 'SOC', 'SAL')
  AND codigo NOT IN (
		SELECT
			familias.prefix || LPAD(((stem_ord - 1) * 4 + nivel.nivel_ord)::text, 2, '0') AS codigo
		FROM familias
		CROSS JOIN LATERAL unnest(familias.stems) WITH ORDINALITY AS stem_data(stem, stem_ord)
		CROSS JOIN (VALUES (1), (2), (3), (4)) AS nivel(nivel_ord)
  );

COMMIT;
