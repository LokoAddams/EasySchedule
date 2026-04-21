BEGIN;

WITH carreras_base(universidad_codigo, nombre, codigo, active) AS (
    VALUES
        -- Universidad Privada Boliviana
        ('UPB', 'Ingenieria de Sistemas Computacionales', 'SISCOMP', TRUE),
        ('UPB', 'Ingenieria Comercial', 'INGCOM', TRUE),
          ('UPB', 'Administracion de Empresas', 'ADMEMP', TRUE),
        ('UPB', 'Ingenieria Financiera', 'INGFIN', TRUE),

        -- Universidad Catolica Boliviana
        ('UCB', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UCB', 'Ingenieria Civil', 'CIV', TRUE),
          ('UCB', 'Arquitectura', 'ARQ', TRUE),
        ('UCB', 'Psicologia', 'PSI', TRUE),

        -- Universidad Mayor de San Simon
        ('UMSS', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UMSS', 'Ingenieria Industrial', 'IND', TRUE),
        ('UMSS', 'Derecho', 'DER', TRUE),
        ('UMSS', 'Medicina', 'MED', TRUE)
)
UPDATE carreras c
SET codigo = cb.codigo,
    active = cb.active
FROM universidades u
JOIN carreras_base cb ON cb.universidad_codigo = u.codigo
WHERE c.universidad_id = u.id
  AND u.active = TRUE
  AND c.nombre = cb.nombre;

WITH carreras_base(universidad_codigo, nombre, codigo, active) AS (
    VALUES
        ('UPB', 'Ingenieria de Sistemas Computacionales', 'SISCOMP', TRUE),
        ('UPB', 'Ingenieria Comercial', 'INGCOM', TRUE),
        ('UPB', 'Administracion de Empresas', 'ADMEMP', TRUE),
        ('UPB', 'Ingenieria Financiera', 'INGFIN', TRUE),
        ('UCB', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UCB', 'Ingenieria Civil', 'CIV', TRUE),
        ('UCB', 'Arquitectura', 'ARQ', TRUE),
        ('UCB', 'Psicologia', 'PSI', TRUE),
        ('UMSS', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UMSS', 'Ingenieria Industrial', 'IND', TRUE),
        ('UMSS', 'Derecho', 'DER', TRUE),
        ('UMSS', 'Medicina', 'MED', TRUE)
)
UPDATE carreras c
SET nombre = cb.nombre,
    active = cb.active
FROM universidades u
JOIN carreras_base cb ON cb.universidad_codigo = u.codigo
WHERE c.universidad_id = u.id
  AND u.active = TRUE
  AND c.codigo = cb.codigo;

INSERT INTO carreras (universidad_id, nombre, codigo, active)
SELECT u.id, cb.nombre, cb.codigo, cb.active
FROM universidades u
JOIN (
    VALUES
        ('UPB', 'Ingenieria de Sistemas Computacionales', 'SISCOMP', TRUE),
        ('UPB', 'Ingenieria Comercial', 'INGCOM', TRUE),
        ('UPB', 'Administracion de Empresas', 'ADMEMP', TRUE),
        ('UPB', 'Ingenieria Financiera', 'INGFIN', TRUE),
        ('UCB', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UCB', 'Ingenieria Civil', 'CIV', TRUE),
        ('UCB', 'Arquitectura', 'ARQ', TRUE),
        ('UCB', 'Psicologia', 'PSI', TRUE),
        ('UMSS', 'Ingenieria de Sistemas', 'SIS', TRUE),
        ('UMSS', 'Ingenieria Industrial', 'IND', TRUE),
        ('UMSS', 'Derecho', 'DER', TRUE),
        ('UMSS', 'Medicina', 'MED', TRUE)
) AS cb(universidad_codigo, nombre, codigo, active) ON cb.universidad_codigo = u.codigo
WHERE u.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM carreras c
      WHERE c.universidad_id = u.id
        AND (c.codigo = cb.codigo OR c.nombre = cb.nombre)
  );

UPDATE carreras c
SET active = FALSE
FROM universidades u
WHERE c.universidad_id = u.id
  AND u.codigo IN ('UPB', 'UCB', 'UMSS')
  AND NOT EXISTS (
      SELECT 1
      FROM (
          VALUES
              ('UPB', 'SISCOMP'),
              ('UPB', 'INGCOM'),
              ('UPB', 'ADMEMP'),
              ('UPB', 'INGFIN'),
              ('UCB', 'SIS'),
              ('UCB', 'CIV'),
              ('UCB', 'ARQ'),
              ('UCB', 'PSI'),
              ('UMSS', 'SIS'),
              ('UMSS', 'IND'),
              ('UMSS', 'DER'),
              ('UMSS', 'MED')
      ) AS permitidas(universidad_codigo, carrera_codigo)
      WHERE permitidas.universidad_codigo = u.codigo
        AND permitidas.carrera_codigo = c.codigo
  )
  AND c.active = TRUE;

COMMIT;
