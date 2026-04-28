BEGIN;

WITH carreras_codigo AS (
	SELECT DISTINCT c.codigo
	FROM carreras c
	JOIN universidades u ON u.id = c.universidad_id
	WHERE c.active = TRUE
	  AND u.active = TRUE
),
-- 1. CREAMOS NUESTRO DICCIONARIO DE MATERIAS REALES
nombres_reales AS (
	SELECT * FROM (VALUES
		-- Semestre 1
		(1, 1, 'Calculo I'), (1, 2, 'Algebra Basica'), (1, 3, 'Fisica Basica'), (1, 4, 'Introduccion a la Programacion'),
		-- Semestre 2
		(2, 1, 'Calculo II'), (2, 2, 'Algebra Lineal'), (2, 3, 'Fisica II'), (2, 4, 'Programacion Orientada a Objetos'),
		-- Semestre 3
		(3, 1, 'Ecuaciones Diferenciales'), (3, 2, 'Estadistica I'), (3, 3, 'Estructura de Datos'), (3, 4, 'Arquitectura de Computadoras'),
		-- Semestre 4
		(4, 1, 'Metodos Numericos'), (4, 2, 'Bases de Datos I'), (4, 3, 'Redes de Computadoras I'), (4, 4, 'Sistemas Operativos I'),
		-- Semestre 5
		(5, 1, 'Investigacion Operativa'), (5, 2, 'Bases de Datos II'), (5, 3, 'Ingenieria de Software'), (5, 4, 'Sistemas de Informacion I'),
		-- Semestre 6
		(6, 1, 'Redes de Computadoras II'), (6, 2, 'Diseno Grafico'), (6, 3, 'Sistemas de Informacion II'), (6, 4, 'Inteligencia Artificial'),
		-- Semestre 7
		(7, 1, 'Ingenieria Economica'), (7, 2, 'Taller de Sistemas de Informacion'), (7, 3, 'Sistemas Expertos'), (7, 4, 'Seguridad de Sistemas'),
		-- Semestre 8
		(8, 1, 'Evaluacion de Proyectos'), (8, 2, 'Auditoria de Sistemas'), (8, 3, 'Simulacion de Sistemas'), (8, 4, 'Practicas Industriales')
	) AS t(semestre, numero, nombre_materia)
),
materias_regular AS (
	-- 2. CRUZAMOS LAS CARRERAS CON LOS NOMBRES REALES
	SELECT
		cc.codigo || '-S' || nr.semestre::TEXT || '-M' || nr.numero::TEXT AS codigo,
		nr.nombre_materia AS nombre,
		CASE
			WHEN nr.semestre <= 2 THEN 5
			ELSE 4
		END AS creditos,
		TRUE AS active
	FROM carreras_codigo cc
	CROSS JOIN nombres_reales nr

	UNION ALL

	SELECT
		cc.codigo || '-S9-M' || m.numero::TEXT AS codigo,
		CASE
			WHEN m.numero = 1 THEN 'Integracion Profesional'
			ELSE 'Electiva de Profundizacion'
		END AS nombre,
		4 AS creditos,
		TRUE AS active
	FROM carreras_codigo cc
	JOIN (VALUES (1),(2)) AS m(numero) ON TRUE
),
materias_tg AS (
	SELECT
		cc.codigo || '-TG1' AS codigo,
		'Taller de Grado I' AS nombre,
		5 AS creditos,
		TRUE AS active
	FROM carreras_codigo cc

	UNION ALL

	SELECT
		cc.codigo || '-TG2' AS codigo,
		'Taller de Grado II' AS nombre,
		6 AS creditos,
		TRUE AS active
	FROM carreras_codigo cc
),
materias_base AS (
	SELECT * FROM materias_regular
	UNION ALL
	SELECT * FROM materias_tg
)
INSERT INTO materias (codigo, nombre, creditos, active)
SELECT mb.codigo, mb.nombre, mb.creditos, mb.active
FROM materias_base mb
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
	creditos = EXCLUDED.creditos,
	active = EXCLUDED.active;

COMMIT;