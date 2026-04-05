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

INSERT INTO universidades (nombre, codigo, active)
SELECT b.nombre, b.codigo, b.active
FROM (
    VALUES
        ('Universidad Privada Boliviana', 'UPB', TRUE),
        ('Universidad Catolica Boliviana', 'UCB', TRUE),
        ('Universidad Mayor de San Simon', 'UMSS', TRUE)
) AS b(nombre, codigo, active)
WHERE NOT EXISTS (
    SELECT 1
    FROM universidades u
    WHERE u.codigo = b.codigo OR u.nombre = b.nombre
);

COMMIT;
