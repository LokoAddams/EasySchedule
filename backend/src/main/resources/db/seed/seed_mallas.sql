BEGIN;

INSERT INTO mallas (carrera_id, version, nombre, active)
SELECT c.id, v.version, 'Malla ' || v.version, TRUE
FROM carreras c
JOIN universidades u ON u.id = c.universidad_id
CROSS JOIN (
    VALUES
        ('2017'),
        ('2024')
) AS v(version)
WHERE u.codigo IN ('UPB', 'UCB', 'UMSS')
  AND c.nombre IN (
      'Ingenieria de Sistemas',
      'Ingenieria Civil',
      'Administracion de Empresas',
      'Ingenieria Comercial',
      'Ingenieria Financiera',
      'Psicologia'
  )
ON CONFLICT (carrera_id, version) DO UPDATE
SET nombre = EXCLUDED.nombre,
    active = EXCLUDED.active;

COMMIT;
