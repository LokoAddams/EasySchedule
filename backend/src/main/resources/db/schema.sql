BEGIN;

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(50) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    token_version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS universidades (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS carreras (
    id BIGSERIAL PRIMARY KEY,
    universidad_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_carreras_universidad FOREIGN KEY (universidad_id) REFERENCES universidades(id) ON DELETE RESTRICT,
    CONSTRAINT uq_carreras_universidad_nombre UNIQUE (universidad_id, nombre),
    CONSTRAINT uq_carreras_universidad_codigo UNIQUE (universidad_id, codigo)
);

CREATE TABLE IF NOT EXISTS mallas (
    id BIGSERIAL PRIMARY KEY,
    carrera_id BIGINT NOT NULL,
    version VARCHAR(30) NOT NULL,
    nombre VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_mallas_carrera FOREIGN KEY (carrera_id) REFERENCES carreras(id) ON DELETE RESTRICT,
    CONSTRAINT uq_mallas_carrera_version UNIQUE (carrera_id, version)
);

CREATE TABLE IF NOT EXISTS materias (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    creditos SMALLINT NOT NULL CHECK (creditos > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS malla_materia (
    id BIGSERIAL PRIMARY KEY,
    malla_id BIGINT NOT NULL,
    materia_id BIGINT NOT NULL,
    semestre_sugerido SMALLINT NOT NULL CHECK (semestre_sugerido BETWEEN 1 AND 50),
    CONSTRAINT fk_malla_materia_malla FOREIGN KEY (malla_id) REFERENCES mallas(id) ON DELETE CASCADE,
    CONSTRAINT fk_malla_materia_materia FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE RESTRICT,
    CONSTRAINT uq_malla_materia UNIQUE (malla_id, materia_id)
);

CREATE TABLE IF NOT EXISTS prerequisitos (
    id BIGSERIAL PRIMARY KEY,
    malla_materia_id BIGINT NOT NULL,
    prereq_malla_materia_id BIGINT NOT NULL,
    CONSTRAINT fk_prereq_malla_materia FOREIGN KEY (malla_materia_id) REFERENCES malla_materia(id) ON DELETE CASCADE,
    CONSTRAINT fk_prereq_prereq_malla_materia FOREIGN KEY (prereq_malla_materia_id) REFERENCES malla_materia(id) ON DELETE RESTRICT,
    CONSTRAINT ck_prereq_distintos CHECK (malla_materia_id <> prereq_malla_materia_id),
    CONSTRAINT uq_prereq UNIQUE (malla_materia_id, prereq_malla_materia_id)
);

CREATE TABLE IF NOT EXISTS student_profiles (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    correo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100),
    apellido VARCHAR(100),
    carnet_identidad VARCHAR(30) UNIQUE,
    fecha_nacimiento DATE,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    semestre_actual SMALLINT,
    universidad_id BIGINT,
    carrera_id BIGINT,
    malla_id BIGINT,
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_profiles_universidad FOREIGN KEY (universidad_id) REFERENCES universidades(id) ON DELETE SET NULL,
    CONSTRAINT fk_student_profiles_carrera FOREIGN KEY (carrera_id) REFERENCES carreras(id) ON DELETE SET NULL,
    CONSTRAINT fk_student_profiles_malla FOREIGN KEY (malla_id) REFERENCES mallas(id) ON DELETE SET NULL,
    CONSTRAINT ck_student_semestre_actual CHECK (semestre_actual IS NULL OR semestre_actual BETWEEN 1 AND 50)
);

ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS username VARCHAR(20);
ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS correo VARCHAR(50);
ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS universidad_id BIGINT;
ALTER TABLE student_profiles ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS estado_materia_estudiante (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    malla_materia_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_estado_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_estado_malla_materia FOREIGN KEY (malla_materia_id) REFERENCES malla_materia(id) ON DELETE RESTRICT,
    CONSTRAINT ck_estado_materia CHECK (estado IN ('aprobada', 'pendiente', 'cursando')),
    CONSTRAINT uq_estado_materia_estudiante UNIQUE (user_id, malla_materia_id)
);

CREATE TABLE IF NOT EXISTS ofertas (
    id BIGSERIAL PRIMARY KEY,
    malla_materia_id BIGINT NOT NULL,
    semestre VARCHAR(30) NOT NULL,
    paralelo VARCHAR(20),
    horario_json JSONB NOT NULL,
    docente VARCHAR(150),
    aula VARCHAR(100),
    fecha_creacion TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_ofertas_malla_materia FOREIGN KEY (malla_materia_id) REFERENCES malla_materia(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ofertas_horario_json_array CHECK (jsonb_typeof(horario_json) = 'array'),
    CONSTRAINT uq_ofertas UNIQUE (malla_materia_id, semestre, paralelo)
);

CREATE TABLE IF NOT EXISTS toma_materia_estudiante (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    oferta_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'inscrita',
    fecha_inscripcion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_toma_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_toma_oferta FOREIGN KEY (oferta_id) REFERENCES ofertas(id) ON DELETE CASCADE,
    CONSTRAINT ck_toma_estado CHECK (estado IN ('inscrita', 'retirada', 'aprobada', 'reprobada')),
    CONSTRAINT uq_toma_user_oferta UNIQUE (user_id, oferta_id)
);

CREATE TABLE IF NOT EXISTS horarios_recomendados (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    semestre VARCHAR(30) NOT NULL,
    json_resultado JSONB NOT NULL,
    fecha_generacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_horarios_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_carreras_universidad_id ON carreras(universidad_id);
CREATE INDEX IF NOT EXISTS idx_mallas_carrera_id ON mallas(carrera_id);
CREATE INDEX IF NOT EXISTS idx_malla_materia_malla_id ON malla_materia(malla_id);
CREATE INDEX IF NOT EXISTS idx_malla_materia_materia_id ON malla_materia(materia_id);
CREATE INDEX IF NOT EXISTS idx_prereq_malla_materia_id ON prerequisitos(malla_materia_id);
CREATE INDEX IF NOT EXISTS idx_prereq_prereq_malla_materia_id ON prerequisitos(prereq_malla_materia_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_carrera_id ON student_profiles(carrera_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_malla_id ON student_profiles(malla_id);
CREATE INDEX IF NOT EXISTS idx_student_profiles_universidad_id ON student_profiles(universidad_id);
CREATE INDEX IF NOT EXISTS idx_ofertas_malla_materia_id ON ofertas(malla_materia_id);
CREATE INDEX IF NOT EXISTS idx_ofertas_horario_json_gin ON ofertas USING GIN (horario_json);
CREATE INDEX IF NOT EXISTS idx_toma_user_id ON toma_materia_estudiante(user_id);
CREATE INDEX IF NOT EXISTS idx_toma_oferta_id ON toma_materia_estudiante(oferta_id);
CREATE INDEX IF NOT EXISTS idx_toma_estado ON toma_materia_estudiante(estado);
CREATE INDEX IF NOT EXISTS idx_estado_user_id ON estado_materia_estudiante(user_id);
CREATE INDEX IF NOT EXISTS idx_estado_malla_materia_id ON estado_materia_estudiante(malla_materia_id);
CREATE INDEX IF NOT EXISTS idx_horarios_user_id ON horarios_recomendados(user_id);
CREATE INDEX IF NOT EXISTS idx_horarios_json_gin ON horarios_recomendados USING GIN (json_resultado);

INSERT INTO universidades (nombre, codigo, active)
VALUES
    ('Universidad Privada Boliviana', 'UPB', TRUE),
    ('Universidad Catolica Boliviana', 'UCB', TRUE),
    ('Universidad Mayor de San Simon', 'UMSS', TRUE)
ON CONFLICT (codigo) DO NOTHING;

COMMIT;
