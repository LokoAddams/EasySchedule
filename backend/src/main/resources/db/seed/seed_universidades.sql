BEGIN;

WITH base(nombre, codigo, active) AS (
    VALUES
        ('Universidad Privada Boliviana', 'UPB', TRUE),
        ('Universidad Catolica Boliviana', 'UCB', TRUE),
        ('Universidad Mayor de San Simon', 'UMSS', TRUE)
)
UPDATE universidades u
SET codigo = b.codigo,
    active = b.active
FROM base b
WHERE u.nombre = b.nombre;

WITH base(nombre, codigo, active) AS (
    VALUES
        ('Universidad Privada Boliviana', 'UPB', TRUE),
        ('Universidad Catolica Boliviana', 'UCB', TRUE),
        ('Universidad Mayor de San Simon', 'UMSS', TRUE)
)
INSERT INTO universidades (nombre, codigo, active)
SELECT b.nombre, b.codigo, b.active
FROM base b
WHERE NOT EXISTS (
    SELECT 1
    FROM universidades u
    WHERE u.codigo = b.codigo OR u.nombre = b.nombre
)
ON CONFLICT (codigo) DO NOTHING;

UPDATE universidades
SET active = FALSE
WHERE codigo NOT IN ('UPB', 'UCB', 'UMSS');

COMMIT;
