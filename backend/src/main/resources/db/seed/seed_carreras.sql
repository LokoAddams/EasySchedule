BEGIN;

WITH carreras_base(nombre, codigo, active) AS (
    VALUES
        ('Ingenieria de Sistemas', 'SIS', TRUE),
        ('Ingenieria Civil', 'CIV', TRUE),
        ('Administracion de Empresas', 'ADM', TRUE),
        ('Ingenieria Comercial', 'COM', TRUE),
        ('Ingenieria Financiera', 'FIN', TRUE),
        ('Psicologia', 'PSI', TRUE)
)
UPDATE carreras c
SET codigo = cb.codigo,
    active = cb.active
FROM universidades u
JOIN carreras_base cb ON TRUE
WHERE c.universidad_id = u.id
  AND u.codigo IN ('UPB', 'UCB', 'UMSS')
  AND c.nombre = cb.nombre;

INSERT INTO carreras (universidad_id, nombre, codigo, active)
SELECT u.id, cb.nombre, cb.codigo, cb.active
FROM universidades u
JOIN (
    VALUES
        ('Ingenieria de Sistemas', 'SIS', TRUE),
        ('Ingenieria Civil', 'CIV', TRUE),
        ('Administracion de Empresas', 'ADM', TRUE),
        ('Ingenieria Comercial', 'COM', TRUE),
        ('Ingenieria Financiera', 'FIN', TRUE),
        ('Psicologia', 'PSI', TRUE)
) AS cb(nombre, codigo, active) ON TRUE
WHERE u.codigo IN ('UPB', 'UCB', 'UMSS')
  AND NOT EXISTS (
      SELECT 1
      FROM carreras c
      WHERE c.universidad_id = u.id
        AND (c.codigo = cb.codigo OR c.nombre = cb.nombre)
  );

COMMIT;
