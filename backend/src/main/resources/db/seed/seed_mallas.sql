BEGIN;

WITH mallas_base(universidad_codigo, carrera_codigo, version, nombre, active) AS (
    VALUES
        ('UPB', 'SISCOMP', '2020', 'Malla 2020', TRUE),
        ('UPB', 'SISCOMP', '2025', 'Malla 2025', TRUE),
        ('UPB', 'INGCOM', '2019', 'Malla 2019', TRUE),
        ('UPB', 'INGCOM', '2024', 'Malla 2024', TRUE),
  ('UPB', 'ADMEMP', '2018', 'Malla 2018', TRUE),
  ('UPB', 'ADMEMP', '2023', 'Malla 2023', TRUE),
        ('UPB', 'INGFIN', '2021', 'Malla 2021', TRUE),
        ('UPB', 'INGFIN', '2024', 'Malla 2024', TRUE),

        ('UCB', 'SIS', '2017', 'Malla 2017', TRUE),
        ('UCB', 'SIS', '2024', 'Malla 2024', TRUE),
        ('UCB', 'CIV', '2018', 'Malla 2018', TRUE),
        ('UCB', 'CIV', '2023', 'Malla 2023', TRUE),
  ('UCB', 'ARQ', '2019', 'Malla 2019', TRUE),
  ('UCB', 'ARQ', '2024', 'Malla 2024', TRUE),
        ('UCB', 'PSI', '2017', 'Malla 2017', TRUE),
        ('UCB', 'PSI', '2022', 'Malla 2022', TRUE),

        ('UMSS', 'SIS', '2016', 'Malla 2016', TRUE),
        ('UMSS', 'SIS', '2023', 'Malla 2023', TRUE),
        ('UMSS', 'IND', '2015', 'Malla 2015', TRUE),
        ('UMSS', 'IND', '2022', 'Malla 2022', TRUE),
        ('UMSS', 'DER', '2014', 'Malla 2014', TRUE),
  ('UMSS', 'DER', '2021', 'Malla 2021', TRUE),
  ('UMSS', 'MED', '2013', 'Malla 2013', TRUE),
  ('UMSS', 'MED', '2020', 'Malla 2020', TRUE)
)
INSERT INTO mallas (carrera_id, version, nombre, active)
SELECT c.id, mb.version, mb.nombre, mb.active
FROM mallas_base mb
JOIN universidades u ON u.codigo = mb.universidad_codigo AND u.active = TRUE
JOIN carreras c ON c.universidad_id = u.id AND c.codigo = mb.carrera_codigo AND c.active = TRUE
ON CONFLICT (carrera_id, version) DO UPDATE
SET nombre = EXCLUDED.nombre,
    active = EXCLUDED.active;

UPDATE mallas m
SET active = FALSE
FROM carreras c
JOIN universidades u ON u.id = c.universidad_id
WHERE m.carrera_id = c.id
  AND c.active = TRUE
  AND u.active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM (
          VALUES
              ('UPB', 'SISCOMP', '2020'),
              ('UPB', 'SISCOMP', '2025'),
              ('UPB', 'INGCOM', '2019'),
              ('UPB', 'INGCOM', '2024'),
              ('UPB', 'ADMEMP', '2018'),
              ('UPB', 'ADMEMP', '2023'),
              ('UPB', 'INGFIN', '2021'),
              ('UPB', 'INGFIN', '2024'),
              ('UCB', 'SIS', '2017'),
              ('UCB', 'SIS', '2024'),
              ('UCB', 'CIV', '2018'),
              ('UCB', 'CIV', '2023'),
              ('UCB', 'ARQ', '2019'),
              ('UCB', 'ARQ', '2024'),
              ('UCB', 'PSI', '2017'),
              ('UCB', 'PSI', '2022'),
              ('UMSS', 'SIS', '2016'),
              ('UMSS', 'SIS', '2023'),
              ('UMSS', 'IND', '2015'),
              ('UMSS', 'IND', '2022'),
              ('UMSS', 'DER', '2014'),
              ('UMSS', 'DER', '2021'),
              ('UMSS', 'MED', '2013'),
              ('UMSS', 'MED', '2020')
      ) AS permitidas(universidad_codigo, carrera_codigo, version)
      WHERE permitidas.universidad_codigo = u.codigo
        AND permitidas.carrera_codigo = c.codigo
        AND permitidas.version = m.version
  )
  AND m.active = TRUE;

UPDATE mallas m
SET active = FALSE
FROM carreras c
JOIN universidades u ON u.id = c.universidad_id
WHERE m.carrera_id = c.id
  AND u.codigo NOT IN ('UPB', 'UCB', 'UMSS')
  AND m.active = TRUE;

COMMIT;
