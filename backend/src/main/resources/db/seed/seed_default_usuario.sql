BEGIN;

-- Usuario por defecto:
-- username: estudiante.sis.ucb
-- email: estudiante.sis.ucb@easyschedule.local
-- password (texto plano): password
-- hash bcrypt de password: $2a$10$h0exP0.K2jjmgIS4Svgfn.hn7r9uWsH4KbTp7dijNsxk8tIutvfoi

-- Insert or update user
INSERT INTO users (username, email, password_hash, is_active, token_version, created_at, updated_at)
VALUES ('estudiante.sis.ucb', 'estudiante.sis.ucb@easyschedule.local', '$2a$10$h0exP0.K2jjmgIS4Svgfn.hn7r9uWsH4KbTp7dijNsxk8tIutvfoi', TRUE, 0, NOW(), NOW())
ON CONFLICT (username) DO UPDATE
SET email = EXCLUDED.email, password_hash = EXCLUDED.password_hash, is_active = TRUE, updated_at = NOW();

-- Insert or update student profile if universities and carreras exist
INSERT INTO student_profiles (user_id, username, correo, nombre, apellido, fecha_registro, semestre_actual, universidad_id, carrera_id, malla_id, profile_completed)
SELECT 
    u.id,
    u.username,
    u.email,
    'Estudiante',
    'Sistemas UCB',
    NOW(),
    1,
    (SELECT id FROM universidades WHERE codigo = 'UCB' LIMIT 1),
    (SELECT id FROM carreras WHERE codigo = 'SIS' AND universidad_id = (SELECT id FROM universidades WHERE codigo = 'UCB' LIMIT 1) LIMIT 1),
    (SELECT id FROM mallas WHERE carrera_id = (SELECT id FROM carreras WHERE codigo = 'SIS' AND universidad_id = (SELECT id FROM universidades WHERE codigo = 'UCB' LIMIT 1) LIMIT 1) ORDER BY version DESC LIMIT 1),
    TRUE
FROM users u
WHERE u.username = 'estudiante.sis.ucb'
ON CONFLICT (user_id) DO UPDATE
SET username = EXCLUDED.username, correo = EXCLUDED.correo, nombre = EXCLUDED.nombre, apellido = EXCLUDED.apellido, 
    semestre_actual = EXCLUDED.semestre_actual, universidad_id = EXCLUDED.universidad_id, carrera_id = EXCLUDED.carrera_id,
    malla_id = EXCLUDED.malla_id, profile_completed = EXCLUDED.profile_completed;

COMMIT;

-- Marca materias de 1er semestre como cursando para la malla seleccionada.
WITH resolved_user AS (
    SELECT u.id AS user_id
    FROM users u
    WHERE u.username = 'estudiante.sis.ucb'
),
selected_target AS (
    SELECT
        m.id AS malla_id
    FROM universidades u
    JOIN carreras c ON c.universidad_id = u.id
    JOIN mallas m ON m.carrera_id = c.id
    WHERE u.codigo = 'UCB'
      AND c.codigo = 'SIS'
      AND u.active = TRUE
      AND c.active = TRUE
      AND m.active = TRUE
    ORDER BY m.version DESC, m.id DESC
    LIMIT 1
),
semestre_uno AS (
    SELECT mm.id AS malla_materia_id
    FROM malla_materia mm
    JOIN selected_target st ON st.malla_id = mm.malla_id
    WHERE mm.semestre_sugerido = 1
)
INSERT INTO estado_materia_estudiante (
    user_id,
    malla_materia_id,
    estado,
    fecha_actualizacion
)
SELECT
    ru.user_id,
    s1.malla_materia_id,
    'cursando',
    NOW()
FROM resolved_user ru
JOIN semestre_uno s1 ON TRUE
ON CONFLICT (user_id, malla_materia_id) DO UPDATE
SET estado = EXCLUDED.estado,
    fecha_actualizacion = NOW();

COMMIT;
