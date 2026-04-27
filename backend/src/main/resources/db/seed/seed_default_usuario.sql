BEGIN;

-- Usuario por defecto:
-- username: estudiante.sis.ucb
-- email: estudiante.sis.ucb@easyschedule.local
-- password (texto plano): password
-- hash bcrypt de password
WITH target_academico AS (
    SELECT
        u.id AS universidad_id,
        c.id AS carrera_id,
        m.id AS malla_id,
        c.codigo AS carrera_codigo,
        ROW_NUMBER() OVER (ORDER BY m.version DESC, m.id DESC) AS rn
    FROM universidades u
    JOIN carreras c ON c.universidad_id = u.id
    JOIN mallas m ON m.carrera_id = c.id
    WHERE u.codigo = 'UCB'
      AND c.codigo = 'SIS'
      AND u.active = TRUE
      AND c.active = TRUE
      AND m.active = TRUE
),
selected_target AS (
    SELECT universidad_id, carrera_id, malla_id, carrera_codigo
    FROM target_academico
    WHERE rn = 1
),
upsert_user AS (
    INSERT INTO users (username, email, password_hash, is_active, token_version)
    VALUES (
        'estudiante.sis.ucb',
        'estudiante.sis.ucb@easyschedule.local',
        '$2a$10$h0exP0.K2jjmgIS4Svgfn.hn7r9uWsH4KbTp7dijNsxk8tIutvfoi',
        TRUE,
        0
    )
    ON CONFLICT (username) DO UPDATE
    SET email = EXCLUDED.email,
        password_hash = EXCLUDED.password_hash,
        is_active = TRUE,
        updated_at = NOW()
    RETURNING id, username, email
),
resolved_user AS (
    SELECT id, username, email
    FROM upsert_user
    UNION ALL
    SELECT u.id, u.username, u.email
    FROM users u
    WHERE u.username = 'estudiante.sis.ucb'
      AND NOT EXISTS (SELECT 1 FROM upsert_user)
)
INSERT INTO student_profiles (
    user_id,
    username,
    correo,
    nombre,
    apellido,
    fecha_registro,
    semestre_actual,
    universidad_id,
    carrera_id,
    malla_id,
    profile_completed
)
SELECT
    ru.id,
    ru.username,
    ru.email,
    'Estudiante',
    'Sistemas UCB',
    NOW(),
    1,
    st.universidad_id,
    st.carrera_id,
    st.malla_id,
    TRUE
FROM resolved_user ru
JOIN selected_target st ON TRUE
ON CONFLICT (user_id) DO UPDATE
SET username = EXCLUDED.username,
    correo = EXCLUDED.correo,
    nombre = EXCLUDED.nombre,
    apellido = EXCLUDED.apellido,
    semestre_actual = EXCLUDED.semestre_actual,
    universidad_id = EXCLUDED.universidad_id,
    carrera_id = EXCLUDED.carrera_id,
    malla_id = EXCLUDED.malla_id,
    profile_completed = EXCLUDED.profile_completed;

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
